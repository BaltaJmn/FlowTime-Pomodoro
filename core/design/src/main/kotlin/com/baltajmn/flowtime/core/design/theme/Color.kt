package com.baltajmn.flowtime.core.design.theme

import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.baltajmn.flowtime.core.design.R
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme

/**
 * Cada tema es un color base. Sus esquemas claro y oscuro salen de él con el algoritmo de Material,
 * que da a cada color uno de encima con el contraste suficiente. Antes eran once esquemas escritos a
 * mano, todos claros, y tres no se leían.
 */
enum class AppTheme(val color: Color, @StringRes val label: Int, private val tint: Tint = Tint.COLOR) {
    Blue(Color(0xFF7AABC3), R.string.theme_blue),
    Pink(Color(0xFFF07C83), R.string.theme_pink),
    Grey(Color(0xFF999997), R.string.theme_grey, Tint.GREY),
    Beige(Color(0xFFB1AAA0), R.string.theme_beige),
    Brown(Color(0xFFAF9984), R.string.theme_brown),
    Olive(Color(0xFFB2AC72), R.string.theme_olive),
    Marine(Color(0xFF7C99A8), R.string.theme_marine),
    Green(Color(0xFF95A893), R.string.theme_green),
    Purple(Color(0xFFC6A9C6), R.string.theme_purple),
    Orange(Color(0xFFFF9966), R.string.theme_orange),
    Black(Color(0xFF16161D), R.string.theme_black, Tint.INK);

    fun colorScheme(dark: Boolean): ColorScheme = when (tint) {
        // TonalSpot es el estilo más suave: mantiene el aire pastel de siempre.
        Tint.COLOR -> dynamicColorScheme(seedColor = color, isDark = dark, isAmoled = false, style = PaletteStyle.TonalSpot)
        Tint.GREY -> greyScheme(dark)
        // Como el negro de antes: tinta negra (o blanca, en oscuro) sobre grises.
        Tint.INK -> greyScheme(dark).let { grey ->
            val ink = if (dark) Color.White else Color.Black
            val onInk = if (dark) Color.Black else Color.White
            grey.copy(primary = ink, onPrimary = onInk, tertiary = ink, onTertiary = onInk)
        }
    }

    // TonalSpot le da color a cualquier base, también a un gris casi puro: el gris salía verde y el
    // negro, violeta. Con todas las paletas sacadas del color base, se quedan en gris.
    private fun greyScheme(dark: Boolean) = dynamicColorScheme(
        seedColor = color,
        isDark = dark,
        isAmoled = false,
        primary = color,
        secondary = color,
        tertiary = color,
        neutral = color,
        neutralVariant = color,
        style = PaletteStyle.TonalSpot
    )

    private enum class Tint { COLOR, GREY, INK }
}
