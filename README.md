# Program that lets you input scripts that edit any loaded image

## Usage
First, load an image with the "Load Image" button.
The script on the left gets run on every pixel of the image, the script is in Kotlin.
Scripts have access to these variables:
  - x: X position of the current pixel
  - y: Y position of the current pixel
  - color: Color of the current pixel, Colors have a red, green and blue value, going from 0.0 to 1.0
  - texture: Matrix of all the pixels of the image, To access a specific pixel use `texture[x,y]`

Textures have the ability to go outside of their bounds, if this happens, the values just get looped. So for example if you try to access `texture[513, 512]` but the image is 512x512, it's return the pixel at x:512, y:512.
You can use operators like +, -, *, / between two Colors, and *, / between a Color and an Int.
The return value just has to get put at the last line. So for example if you have a variable with the final pixel value called `result`, your last line would be `result`.

## Building
run `./gradlew :composeApp:createDistributable`
