package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.baltajmn.flowtime.core.design.resources.*
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import org.jetbrains.compose.resources.StringResource

/**
 * Cada tema es un color base. Sus esquemas claro y oscuro salen de él con el algoritmo de Material,
 * que da a cada color uno de encima con el contraste suficiente. Antes eran once esquemas escritos a
 * mano, todos claros, y tres no se leían.
 */
enum class AppTheme(
    val color: Color,
    val label: StringResource,
    private val tint: Tint = Tint.COLOR,
    /** De Pro (#56): sin Pro a la venta no salen; sin comprar, con candado. Uno ya elegido no se quita. */
    val pro: Boolean = false
) {
    Blue(Color(0xFF7AABC3), Res.string.theme_blue),
    Pink(Color(0xFFF07C83), Res.string.theme_pink),
    Grey(Color(0xFF999997), Res.string.theme_grey, Tint.GREY),
    Beige(Color(0xFFB1AAA0), Res.string.theme_beige),
    Brown(Color(0xFFAF9984), Res.string.theme_brown),
    Olive(Color(0xFFB2AC72), Res.string.theme_olive),
    Marine(Color(0xFF7C99A8), Res.string.theme_marine),
    Green(Color(0xFF95A893), Res.string.theme_green),
    Purple(Color(0xFFC6A9C6), Res.string.theme_purple),
    Orange(Color(0xFFFF9966), Res.string.theme_orange),
    Black(Color(0xFF16161D), Res.string.theme_black, Tint.INK),

    /** El regalo de las propinas (#58): solo aparece en la lista para quien ha dejado alguna. */
    Supporter(Color(0xFFE2B04A), Res.string.theme_supporter),

    Lavender(Color(0xFFA99BD6), Res.string.theme_lavender, pro = true),
    Mint(Color(0xFF8FCFB5), Res.string.theme_mint, pro = true),
    Coral(Color(0xFFF28B6E), Res.string.theme_coral, pro = true),
    Sand(Color(0xFFD8C08E), Res.string.theme_sand, pro = true),
    Night(Color(0xFF2E3A59), Res.string.theme_night, pro = true),
    Cherry(Color(0xFFC2445E), Res.string.theme_cherry, pro = true);

    fun colorScheme(dark: Boolean): ColorScheme = when (tint) {
        // TonalSpot es el estilo más suave: mantiene el aire pastel de siempre.
        Tint.COLOR -> dynamicColorScheme(seedColor = color, isDark = dark, isAmoled = false, style = PaletteStyle.TonalSpot)
        Tint.GREY -> greyScheme(dark)
        // Como el negro de antes: tinta negra (o blanca, en oscuro) sobre grises.
        Tint.INK -> greyScheme(dark).let { grey ->
            val ink = if (dark) Color.White else Color.Black
            val onInk = if (dark) Color.Black else Color.White
            grey.copy(primary = ink, onPrimary = onInk)
        }
    }

    // TonalSpot le da color a cualquier base, también a un gris casi puro: el gris salía verde y el
    // negro, violeta. Con todas las paletas sacadas del color base, se quedan en gris. Menos tertiary,
    // el color del descanso: en gris era el mismo que el del trabajo y el anillo no los distinguía.
    private fun greyScheme(dark: Boolean) = dynamicColorScheme(
        seedColor = color,
        isDark = dark,
        isAmoled = false,
        primary = color,
        secondary = color,
        tertiary = REST,
        neutral = color,
        neutralVariant = color,
        style = PaletteStyle.TonalSpot
    )

    private enum class Tint { COLOR, GREY, INK }
}

/** El descanso de los temas sin color: un gris azulado, apagado como ellos. */
private val REST = Color(0xFF7C99A8)
