package com.baltajmn.flowtime.core.design.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun FlowTimeTheme(
    appearance: Appearance,
    content: @Composable () -> Unit
) {
    val dark = appearance.isDark(isSystemInDarkTheme())
    val system = if (appearance.dynamicColor) systemColorScheme(dark) else null
    val colorScheme = system ?: remember(appearance.theme, dark) { appearance.theme.colorScheme(dark) }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        content = content
    )
}

/** Los colores del fondo de pantalla (Material You, desde Android 12); null donde no los hay. */
@Composable
expect fun systemColorScheme(dark: Boolean): ColorScheme?

/** Un tema concreto, sin mirar los ajustes: para vistas previas y capturas. */
@Composable
fun FlowTimeTheme(
    theme: AppTheme = AppTheme.Blue,
    dark: Boolean = false,
    content: @Composable () -> Unit
) = FlowTimeTheme(
    appearance = Appearance(theme = theme, darkMode = if (dark) DarkMode.DARK else DarkMode.LIGHT),
    content = content
)
