package com.github.kittinunf.app.theme

import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb

private val DarkColorPalette = darkColors(
    primary = KNewsColor.orangeLight,
    primaryVariant = KNewsColor.orange,
    secondary = Teal200
)

private val LightColorPalette = lightColors(
    primary = KNewsColor.orangeLight,
    primaryVariant = KNewsColor.orange,
    secondary = Teal200

    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun KNewsTheme(darkTheme: Boolean = isSystemInDarkTheme(), windows: Window? = null, content: @Composable () -> Unit) {
    MaterialTheme(
        colors = LightColorPalette,
        typography = typography,
        shapes = Shapes,
        content = {
            windows?.statusBarColor = MaterialTheme.colors.primaryVariant.toArgb()
            content()
        }
    )
}
