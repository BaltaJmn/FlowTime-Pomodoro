package com.baltajmn.flowtime.core.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertEquals
import org.junit.Test

class TagPaletteTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance() + 0.05f
        val lb = b.luminance() + 0.05f
        return max(la, lb) / min(la, lb)
    }

    @Test
    fun `cada color tiene su version clara y oscura`() {
        assertEquals(TagPalette.COUNT, TagPalette.light.size)
        assertEquals(TagPalette.COUNT, TagPalette.dark.size)
    }

    @Test
    fun `el texto sobre cada color se lee en claro y en oscuro`() {
        val failures = listOf(false, true).flatMap { dark ->
            (0 until TagPalette.COUNT).mapNotNull { index ->
                val ratio = contrast(TagPalette.onColor(dark), TagPalette.color(index, dark))
                if (ratio >= 4.5f) null else "${if (dark) "oscuro" else "claro"} $index: ${"%.2f".format(ratio)}"
            }
        }

        assertEquals(emptyList<String>(), failures)
    }
}
