package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import com.baltajmn.flowtime.core.design.resources.*

// Composable: en Compose Multiplatform las fuentes de los recursos se cargan dentro de la composición.
// Tres pesos en toda la app: 300 el reloj, 400 el texto y 600 títulos, valores y etiquetas. Lo
// secundario se distingue por el color, no por ir más fino.
val LargeTitle: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Mulish,
        fontWeight = FontWeight.W600,
        fontSize = 27.sp,
        letterSpacing = (-0.3).sp
    )

val Title: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Mulish,
        fontWeight = FontWeight.W600,
        fontSize = 24.sp
    )

val SmallTitle: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Mulish,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp
    )

val SubBody: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Mulish,
        fontWeight = FontWeight.W400,
        fontSize = 15.sp
    )

/** El título de cada hoja y diálogo propio: uno solo, en 600 como los de pantalla, pero menor. */
val SheetTitle: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Mulish,
        fontWeight = FontWeight.W600,
        fontSize = 22.sp,
        letterSpacing = (-0.2).sp
    )

val Button: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Mulish,
        fontWeight = FontWeight.W600,
        fontSize = 18.sp
    )

/** El reloj, en el peso más fino: es lo más grande de la pantalla y no tiene que gritar. */
val Timer: TextStyle
    @Composable get() = TextStyle(
        fontFamily = FontFamily.Mulish,
        fontWeight = FontWeight.W300,
        fontSize = 100.sp
    )

/** Pestañas, chips, barra y diálogos de Material con la misma letra, y sus etiquetas en 600. */
val AppTypography: Typography
    @Composable get() {
        val mulish = FontFamily.Mulish
        fun TextStyle.mulish(weight: FontWeight? = fontWeight) = copy(fontFamily = mulish, fontWeight = weight)
        return Typography().run {
            copy(
                displayLarge = displayLarge.mulish(),
                displayMedium = displayMedium.mulish(),
                displaySmall = displaySmall.mulish(),
                headlineLarge = headlineLarge.mulish(),
                headlineMedium = headlineMedium.mulish(),
                // Títulos de diálogo como los de las hojas (SheetTitle): antes 24 sp finos, otra voz.
                headlineSmall = headlineSmall.mulish(FontWeight.W600).copy(fontSize = 22.sp, letterSpacing = (-0.2).sp),
                titleLarge = titleLarge.mulish(),
                titleMedium = titleMedium.mulish(FontWeight.W600),
                titleSmall = titleSmall.mulish(FontWeight.W600),
                bodyLarge = bodyLarge.mulish(),
                bodyMedium = bodyMedium.mulish(),
                bodySmall = bodySmall.mulish(),
                labelLarge = labelLarge.mulish(FontWeight.W600),
                labelMedium = labelMedium.mulish(FontWeight.W600),
                labelSmall = labelSmall.mulish(FontWeight.W600)
            )
        }
    }

/** Un solo fichero variable: cada peso es una posición de su eje wght (200 a 1000). */
val FontFamily.Companion.Mulish: FontFamily
    @Composable get() = FontFamily(
        Font(Res.font.mulish, FontWeight.W300),
        Font(Res.font.mulish, FontWeight.W400),
        Font(Res.font.mulish, FontWeight.W600)
    )
