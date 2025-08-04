package gay.tinya.imageprocessing

import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Window

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ImageProcessing",
    ) {
        val window = ComposeWindow()
        App(window)
    }
}