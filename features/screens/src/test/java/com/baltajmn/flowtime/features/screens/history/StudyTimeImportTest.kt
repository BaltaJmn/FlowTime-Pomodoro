package com.baltajmn.flowtime.features.screens.history

import com.baltajmn.flowtime.features.screens.history.usecases.MAX_MINUTES_PER_DAY
import com.baltajmn.flowtime.features.screens.history.usecases.parseStudyTimeImport
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyTimeImportTest {

    private val sep29 = LocalDate.of(2026, 9, 29)
    private val sep30 = LocalDate.of(2026, 9, 30)

    @Test
    fun `lee el texto que exporta la app`() {
        val parsed = parseStudyTimeImport("29092026: 45\n30092026: 120")

        assertEquals(mapOf(sep29 to 45L, sep30 to 120L), parsed.minutesByDay)
        assertEquals(0, parsed.ignoredLines)
    }

    @Test
    fun `las claves de ajustes se descartan`() {
        val parsed = parseStudyTimeImport("theme_color: 1\nshow_on_board: 0\n29092026: 30")

        assertEquals(mapOf(sep29 to 30L), parsed.minutesByDay)
        assertEquals(2, parsed.ignoredLines)
    }

    @Test
    fun `las fechas imposibles y los valores enormes se descartan`() {
        val parsed = parseStudyTimeImport("32132026: 30\n29092026: 999999\n28092026: 1440")

        assertEquals(mapOf(LocalDate.of(2026, 9, 28) to MAX_MINUTES_PER_DAY), parsed.minutesByDay)
        assertEquals(2, parsed.ignoredLines)
    }

    @Test
    fun `el texto vacio no importa nada`() {
        val parsed = parseStudyTimeImport("   \n\n")

        assertEquals(emptyMap<LocalDate, Long>(), parsed.minutesByDay)
        assertEquals(0, parsed.ignoredLines)
    }

    @Test
    fun `tolera espacios y claves con otros digitos`() {
        val parsed = parseStudyTimeImport("  29092026 :45  \n٣٠٠٩٢٠٢٦: 10")

        assertEquals(mapOf(sep29 to 45L, sep30 to 10L), parsed.minutesByDay)
    }
}
