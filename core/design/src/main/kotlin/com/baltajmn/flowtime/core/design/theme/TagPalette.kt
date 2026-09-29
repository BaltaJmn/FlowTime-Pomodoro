package com.baltajmn.flowtime.core.design.theme

import androidx.compose.ui.graphics.Color

/**
 * Los colores de las etiquetas. Cada etiqueta guarda su índice, no el color: así cada uno tiene su
 * versión para el modo claro (oscura, con texto blanco encima) y para el oscuro (clara, con texto
 * oscuro encima), las dos con un contraste de al menos 4,5:1.
 */
object TagPalette {
    const val COUNT = 10

    internal val light = listOf(
        Color(0xFF1565C0), // azul
        Color(0xFF00796B), // verde azulado
        Color(0xFF2E7D32), // verde
        Color(0xFF76701A), // oliva
        Color(0xFF8D6E00), // ámbar
        Color(0xFFBF360C), // naranja
        Color(0xFFC62828), // rojo
        Color(0xFFAD1457), // rosa
        Color(0xFF6A1B9A), // morado
        Color(0xFF5D4037) // marrón
    )

    internal val dark = listOf(
        Color(0xFF90CAF9),
        Color(0xFF80CBC4),
        Color(0xFFA5D6A7),
        Color(0xFFE6EE9C),
        Color(0xFFFFE082),
        Color(0xFFFFAB91),
        Color(0xFFEF9A9A),
        Color(0xFFF48FB1),
        Color(0xFFCE93D8),
        Color(0xFFBCAAA4)
    )

    fun color(index: Int, dark: Boolean): Color = (if (dark) this.dark else light)[index.mod(COUNT)]

    /** El texto que va encima del color. */
    fun onColor(dark: Boolean): Color = if (dark) Color(0xFF111316) else Color.White
}
