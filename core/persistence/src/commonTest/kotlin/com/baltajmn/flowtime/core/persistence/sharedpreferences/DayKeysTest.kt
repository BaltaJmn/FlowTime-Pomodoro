package com.baltajmn.flowtime.core.persistence.sharedpreferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class DayKeysTest {

    @Test
    fun `la clave usa digitos latinos aunque el idioma del sistema use otros`() {
        val date = LocalDate(2026, 9, 29)

        assertEquals("29092026", DayKeys.of(date))
    }

    @Test
    fun `se leen claves guardadas con digitos arabes`() {
        assertEquals("29092026", DayKeys.normalize("٢٩٠٩٢٠٢٦"))
        assertEquals(LocalDate(2026, 9, 29), DayKeys.parse("٢٩٠٩٢٠٢٦"))
    }

    @Test
    fun `se leen claves guardadas con digitos persas`() {
        assertEquals(LocalDate(2026, 9, 29), DayKeys.parse("۲۹۰۹۲۰۲۶"))
    }

    @Test
    fun `las claves de ajustes no son dias`() {
        assertNull(DayKeys.parse("theme_color"))
        assertNull(DayKeys.parse("show_on_board"))
        assertNull(DayKeys.parse("1234567"))
        assertNull(DayKeys.parse("123456789"))
    }

    @Test
    fun `una fecha imposible no es un dia`() {
        assertNull(DayKeys.parse("32132026"))
    }
}
