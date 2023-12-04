package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.baltajmn.flowtime.core.design.theme.Blue
import com.baltajmn.flowtime.core.design.theme.DarkBlue
import com.baltajmn.flowtime.core.design.theme.Surface
import com.google.accompanist.systemuicontroller.rememberSystemUiController

private val ColorPalette = ColorScheme(
    primary = Blue,
    secondary = DarkBlue,
    tertiary = Surface,
    error = DarkBlue,
    background = Color.Black,
    onBackground = Color.White,
    primaryContainer = Color.Black,
    onPrimaryContainer = Color.White,
    secondaryContainer = Color.Black,
    onSecondaryContainer = Color.White,
    tertiaryContainer = Color.Black,
    onTertiaryContainer = Color.White,
    errorContainer = Color.Black,
    onErrorContainer = Color.White,
    surface = Color.White,
    onSurface = Color.White,
    surfaceVariant = Color.Black,
    onSurfaceVariant = Color.White,
    outline = Color.Black,
    inverseOnSurface = Color.White,
    inverseSurface = Color.Black,
    inversePrimary = DarkBlue,
    onError = DarkBlue,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    outlineVariant = Color.Black,
    scrim = Color.Black,
    surfaceTint = DarkBlue,
)


@Composable
fun FlowTimeTheme(content: @Composable () -> Unit) {

    val systemUiController = rememberSystemUiController()
    systemUiController.setSystemBarsColor(
        color = ColorPalette.background.copy(alpha = 0.5f),
        darkIcons = false
    )

    MaterialTheme(
        colorScheme = ColorPalette,
        shapes = Shapes,
        content = content
    )

}
