package com.baltajmn.flowtime.core.design.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
fun FlowTimeTheme(
    appearance: Appearance,
    content: @Composable () -> Unit
) {
    val dark = appearance.isDark(isSystemInDarkTheme())
    val context = LocalContext.current
    val colorScheme = if (appearance.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        remember(appearance.theme, dark) { appearance.theme.colorScheme(dark) }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = Shapes,
        content = content
    )
}

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
