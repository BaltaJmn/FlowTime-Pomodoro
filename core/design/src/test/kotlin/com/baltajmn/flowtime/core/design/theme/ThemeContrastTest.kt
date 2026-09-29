package com.baltajmn.flowtime.core.design.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Las parejas de color de texto y fondo que usa la app, en los once temas, en claro y en oscuro.
 * Tres temas eran ilegibles (#28); con los esquemas generados no debería volver a pasar.
 */
class ThemeContrastTest {

    private class Pair(val name: String, val pick: (ColorScheme) -> kotlin.Pair<Color, Color>)

    private val text = listOf(
        Pair("primary sobre surface") { it.primary to it.surface },
        Pair("primary sobre background") { it.primary to it.background },
        Pair("primary sobre primaryContainer") { it.primary to it.primaryContainer },
        Pair("primary sobre una tarjeta") { it.primary to it.surfaceContainerHighest },
        Pair("tertiary sobre primaryContainer") { it.tertiary to it.primaryContainer },
        Pair("tertiary sobre una tarjeta") { it.tertiary to it.surfaceContainerHighest },
        Pair("secondary sobre primaryContainer") { it.secondary to it.primaryContainer },
        Pair("secondary sobre surface") { it.secondary to it.surface },
        Pair("onSurface sobre surface") { it.onSurface to it.surface },
        Pair("onBackground sobre background") { it.onBackground to it.background },
        Pair("onPrimary sobre primary") { it.onPrimary to it.primary }
    )

    // Bordes y elementos que no son texto: 3:1.
    private val shapes = listOf(
        Pair("outline sobre surface") { it.outline to it.surface },
        // El borde de los iconos de ruido en el panel de sonido.
        Pair("onSurfaceVariant sobre primaryContainer") { it.onSurfaceVariant to it.primaryContainer }
    )

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return max(la, lb) / min(la, lb)
    }

    private fun failures(pairs: List<Pair>, minimum: Float) = AppTheme.entries.flatMap { theme ->
        listOf(false, true).flatMap { dark ->
            val scheme = theme.colorScheme(dark)
            pairs.mapNotNull { pair ->
                val (foreground, background) = pair.pick(scheme)
                val ratio = contrast(foreground, background)
                if (ratio >= minimum) null else "$theme ${if (dark) "oscuro" else "claro"}, ${pair.name}: ${"%.2f".format(ratio)}"
            }
        }
    }

    @Test
    fun `todo el texto se lee en todos los temas, en claro y en oscuro`() {
        assertEquals(emptyList<String>(), failures(text, minimum = 4.5f))
    }

    @Test
    fun `los bordes se distinguen del fondo`() {
        assertEquals(emptyList<String>(), failures(shapes, minimum = 3f))
    }
}
