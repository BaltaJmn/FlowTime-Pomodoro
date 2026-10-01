package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

/** El iPhone no tiene colores del fondo de pantalla: siempre el tema elegido. */
@Composable
actual fun systemColorScheme(dark: Boolean): ColorScheme? = null
