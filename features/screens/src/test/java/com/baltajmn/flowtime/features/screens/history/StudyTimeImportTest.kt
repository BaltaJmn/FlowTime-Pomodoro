package com.baltajmn.flowtime.features.screens.history

import com.baltajmn.flowtime.features.screens.history.usecases.ImportMode
import com.baltajmn.flowtime.features.screens.history.usecases.MAX_MINUTES_PER_DAY
import com.baltajmn.flowtime.features.screens.history.usecases.mergeStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.parseStudyTimeImport
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyTimeImportTest {

    @Test
    fun `lee el texto que exporta la app`() {
        val parsed = parseStudyTimeImport("29092026: 45\n30092026: 120")

        assertEquals(mapOf("29092026" to 45L, "30092026" to 120L), parsed.minutesByDay)
        assertEquals(0, parsed.ignoredLines)
    }

    @Test
    fun `las claves de ajustes se descartan`() {
        val parsed = parseStudyTimeImport("theme_color: 1\nshow_on_board: 0\n29092026: 30")

        assertEquals(mapOf("29092026" to 30L), parsed.minutesByDay)
        assertEquals(2, parsed.ignoredLines)
    }

    @Test
    fun `las fechas imposibles y los valores enormes se descartan`() {
        val parsed = parseStudyTimeImport("32132026: 30\n29092026: 999999\n28092026: 1440")

        assertEquals(mapOf("28092026" to MAX_MINUTES_PER_DAY), parsed.minutesByDay)
        assertEquals(2, parsed.ignoredLines)
    }

    @Test
    fun `el texto vacio no importa nada`() {
        val parsed = parseStudyTimeImport("   \n\n")

        assertEquals(emptyMap<String, Long>(), parsed.minutesByDay)
        assertEquals(0, parsed.ignoredLines)
    }

    @Test
    fun `tolera espacios y claves con otros digitos`() {
        val parsed = parseStudyTimeImport("  29092026 :45  \n٣٠٠٩٢٠٢٦: 10")

        assertEquals(mapOf("29092026" to 45L, "30092026" to 10L), parsed.minutesByDay)
    }

    @Test
    fun `sustituir deja los minutos importados`() {
        val merged = mergeStudyTime(
            existing = mapOf("29092026" to 60L),
            imported = mapOf("29092026" to 30L),
            mode = ImportMode.REPLACE
        )

        assertEquals(mapOf("29092026" to 30L), merged)
    }

    @Test
    fun `sumar no pasa de los minutos de un dia`() {
        val merged = mergeStudyTime(
            existing = mapOf("29092026" to 60L, "30092026" to 1400L),
            imported = mapOf("29092026" to 30L, "30092026" to 100L),
            mode = ImportMode.SUM
        )

        assertEquals(mapOf("29092026" to 90L, "30092026" to MAX_MINUTES_PER_DAY), merged)
    }
}
