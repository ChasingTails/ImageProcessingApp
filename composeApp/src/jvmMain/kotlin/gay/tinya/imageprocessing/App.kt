package gay.tinya.imageprocessing

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxTheme
import dev.snipme.highlights.model.SyntaxThemes
import dev.snipme.kodeview.view.CodeEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.ui.tooling.preview.Preview

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import org.jetbrains.skiko.currentNanoTime
import org.jetbrains.skiko.toBufferedImage
import org.jetbrains.skiko.toImage
import java.io.File
import javax.imageio.ImageIO
import javax.script.ScriptEngineManager
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.random.nextUInt

@Composable
@Preview
fun App() {
    MaterialTheme {
        Row(Modifier.fillMaxSize()) {
            var bitmap: ImageBitmap by remember {
                mutableStateOf(
                    ImageBitmap(
                        512,
                        512,
                        ImageBitmapConfig.Rgb565,
                        false,
                        ColorSpaces.Srgb
                    )
                )
            }
            var editedBitmap by rememberSaveable { mutableStateOf(bitmap) }
            var scriptResult by remember { mutableStateOf("") }
            LazyColumn(
                Modifier.fillMaxHeight().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Button(onClick = {
                        OpenImage({ bitmap = it })
                        editedBitmap = ImageProcessing(bitmap)
                    }) {
                        Text("Load Image")
                    }
                    var showOriginal by rememberSaveable { mutableStateOf(true) }
                    Text("Show Original")
                    Checkbox(
                        checked = showOriginal,
                        onCheckedChange = { showOriginal = it },
                    )
                    Text(scriptResult)
                    Box(Modifier.width(500.dp)) {
                        if (showOriginal) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Image(
                                bitmap = editedBitmap,
                                contentDescription = null,
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Button(onClick = {
                        SaveImage(editedBitmap)
                    }) {
                        Text("Save Result")
                    }
                }
            }
            Column(
                Modifier.fillMaxHeight(1f).weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var highlights by remember {
                    mutableStateOf(
                        Highlights.Builder("val newColor = Color(1.0 - color.red, 1.0 - color.green, 1.0 - color.blue)\nnewColor").language(SyntaxLanguage.KOTLIN).theme(
                            SyntaxThemes.notepad()
                        ).build()
                    )
                }
                val scope = rememberCoroutineScope()
                Row(Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        SaveScript(highlights.getCode())
                    }){
                        Text("Save")
                    }
                    /*Button(onClick = {
                        val newScript = OpenScript()
                        if(newScript != null) {
                            println(newScript)
                            highlights.setCode(newScript)
                            highlights = highlights.getBuilder()
                                .code(newScript)
                                .build()
                        }
                    }){
                        Text("Load")
                    }*/
                }
                var currentJob: Job? = null
                CodeEditText(
                    highlights = highlights,
                    onValueChange = { textValue ->
                        scriptResult = "..."
                        highlights = highlights.getBuilder()
                            .code(textValue)
                            .build()

                        currentJob?.cancel()

                        currentJob = scope.launch(Dispatchers.Default) {
                            var resultText = ""
                            val lambda = EvalScript(textValue, { resultText = it })
                            println("${lambda == null}")
                            if (lambda != null) SpecialEffect = lambda
                            val result = ImageProcessing(bitmap)

                            scope.launch(Dispatchers.Main) {
                                editedBitmap = result
                            }
                            scriptResult = resultText
                        }
                    },
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}
class WrappingMatrix<T>(private val grid: List<List<T>>) {
    val width = grid.firstOrNull()?.size ?: 0
    val height = grid.size

    operator fun get(x: Int, y: Int): T {
        if (width == 0 || height == 0) throw IndexOutOfBoundsException("Empty matrix")
        val wrappedX = ((x % width) + width) % width
        val wrappedY = ((y % height) + height) % height
        return grid[wrappedY][wrappedX]
    }

    operator fun set(x: Int, y: Int, value: T) {
        val wrappedX = ((x % width) + width) % width
        val wrappedY = ((y % height) + height) % height
        (grid[wrappedY] as MutableList)[wrappedX] = value
    }

    fun toList(): List<List<T>> = grid
}

data class Color(val red: Double, val green: Double, val blue: Double){
    operator fun plus(other: Color): Color {
        return Color(red + other.red, green + other.green, blue + other.blue)
    }
    operator fun minus(other: Color): Color {
        return Color(red - other.red, green - other.green, blue - other.blue)
    }
    operator fun times(factor: Double): Color {
        return Color(red * factor, green * factor, blue * factor)
    }
    operator fun times(other: Color): Color {
        return Color(red * other.red, green * other.green, blue * other.blue)
    }
    operator fun div(factor: Double): Color {
        return Color(red / factor, green / factor, blue / factor)
    }
    operator fun div(other: Color): Color {
        return Color(red / other.red, green / other.green, blue / other.blue)
    }
}

fun EvalScript(script: String, scriptResult: (String) -> Unit): ((Int, Int, Color, WrappingMatrix<Color>) -> Color)? {
    val engine = ScriptEngineManager().getEngineByExtension("kts")
        ?: throw IllegalStateException("Kotlin engine not found")

    val editedScript = "import gay.tinya.imageprocessing.Color\n" +
            "import gay.tinya.imageprocessing.WrappingMatrix\n" +
            "run<(Int, Int, Color, WrappingMatrix<Color>) -> Color> {\n" +
            "{ x: Int, y: Int, color: Color, texture: WrappingMatrix<Color> ->\n" +
            script +
            "\n}" +
            "\n}"
    val result = try {
        engine.eval(editedScript)
    }
    catch (e: Exception) {
        val lines = e.message?.lines()
        scriptResult((lines?.filter { it.trimStart().startsWith("ERROR")})?.fastJoinToString("\n") ?: "")
        return { _, _, color, _ -> color}
    }
    val fn = result  as? (Int, Int, Color, WrappingMatrix<Color>) -> Color
    if(fn == null) {
        scriptResult("Error: Script did not return a valid function.")
        return { _, _, color, _ -> color}
    }
    scriptResult("Success")
    return fn
}

fun ConvertToSkiaBGRAImage(original: ImageBitmap): Bitmap {
    val originalBitmap = original.asSkiaBitmap()
    val imageInfo = ImageInfo(
        width = originalBitmap.width,
        height = originalBitmap.height,
        colorType = ColorType.BGRA_1010102,
        alphaType = originalBitmap.alphaType,
        colorSpace = originalBitmap.colorSpace,
    )

    val surface = Surface.makeRaster(imageInfo)
    val canvas = surface.canvas

    canvas.drawImage(original.toAwtImage().toImage(), 0f, 0f)

    return surface.makeImageSnapshot().toComposeImageBitmap().asSkiaBitmap()
}

var SpecialEffect: (Int, Int, Color, WrappingMatrix<Color>) -> Color = { x: Int, y: Int, color: Color, texture ->
    val newColor = Color(1.0 - color.red, 1.0 - color.green, 1.0 - color.blue)
    newColor
}

fun ImageProcessing(original: ImageBitmap): ImageBitmap {
    var bitmap = ConvertToSkiaBGRAImage(original)
    var pixels = bitmap.readPixels()
    pixels?.let {
        var newPixels: MutableList<MutableList<Color>> = mutableListOf()
        for (y in 0 until bitmap.height) {
            var pixelLine: MutableList<Color> = mutableListOf()
            for (x in 0 until bitmap.width) {
                val index = (y * bitmap.width + x) * 4
                val b = (pixels[index].toInt() and 0xFF) / 255.0
                val g = (pixels[index + 1].toInt() and 0xFF) / 255.0
                val r = (pixels[index + 2].toInt() and 0xFF) / 255.0

                val pixel = Color(r,g,b)
                pixelLine.add(pixel)
            }
            newPixels.add(pixelLine)
        }

        for (y in 0 until newPixels.size) {
            for (x in 0 until newPixels[0].size) {

                newPixels[y][x] = SpecialEffect(x, y, newPixels[y][x], WrappingMatrix(newPixels))
                val index = (y * bitmap.width + x) * 4
                val b = index
                val g = index + 1
                val r = index + 2
                val a = index + 3

                pixels[r] = (newPixels[y][x].red * 255).toInt().toByte()
                pixels[g] = (newPixels[y][x].green * 255).toInt().toByte()
                pixels[b] = (newPixels[y][x].blue * 255).toInt().toByte()
                pixels[a] = 255.toByte()
            }
        }

        bitmap.installPixels(pixels)
    }



    return bitmap.asComposeImageBitmap()
}


fun OpenImage(changeImage: (ImageBitmap) -> Unit) {
    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = "Open Image"

    val userSelection = fileChooser.showOpenDialog(null)
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.selectedFile
        val inputStream = ImageIO.createImageInputStream(file)
        val readers = ImageIO.getImageReaders(inputStream)
        if (!readers.hasNext()) return
        val reader = readers.next()
        reader.input = inputStream
        val image = reader.read(0)
        changeImage(image.toComposeImageBitmap())
    }
}

fun SaveImage(image: ImageBitmap) {
    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = "Save Image"
    fileChooser.selectedFile = File(Random(currentNanoTime()).nextUInt().toString() + ".png")

    val userSelection = fileChooser.showSaveDialog(null)
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.selectedFile
        val skiaImage = image.asSkiaBitmap()
        val bufferedImage = skiaImage.toBufferedImage()
        ImageIO.write(bufferedImage, "png", file)
    }
}

fun OpenScript(): String? {
    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = "Open Script"
    fileChooser.fileFilter = FileNameExtensionFilter("Shader Script", "shade")

    val userSelection = fileChooser.showOpenDialog(null)
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        val file = fileChooser.selectedFile
        val text = file.readText(Charsets.UTF_8)
        return text
    }
    return null
}

fun SaveScript(script: String) {
    val fileChooser = JFileChooser()
    fileChooser.dialogTitle = "Save Script"
    fileChooser.fileFilter = FileNameExtensionFilter("Shader Script", "shade")

    val userSelection = fileChooser.showSaveDialog(null)
    if (userSelection == JFileChooser.APPROVE_OPTION) {
        var file = fileChooser.selectedFile
        if(!file.name.endsWith(".shade")){
            file = File(file.absolutePath + ".shade")
        }
        val text = file.writeText(script)
    }
}