package com.baltajmn.flowtime.data.stats

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YearRecapTest {

    @Test
    fun `el resumen se ve del 1 de diciembre al 15 de enero`() {
        assertNull(recapYear(LocalDate(2026, 11, 30)))
        assertEquals(2026, recapYear(LocalDate(2026, 12, 1)))
        assertEquals(2026, recapYear(LocalDate(2026, 12, 31)))
        assertEquals(2026, recapYear(LocalDate(2027, 1, 1)))
        assertEquals(2026, recapYear(LocalDate(2027, 1, 15)))
        assertNull(recapYear(LocalDate(2027, 1, 16)))
        assertNull(recapYear(LocalDate(2026, 7, 4)))
    }

    @Test
    fun `sin foco no hay resumen`() {
        assertNull(yearRecap(2026, StatsSummary(), emptyList()))
    }

    @Test
    fun `cuenta horas, dias, mejor racha, mejor dia y dia de la semana`() {
        // Lunes 5, martes 6 y miércoles 7 de octubre cumplen los 60 minutos del objetivo; el lunes 12, no.
        val byDay = mapOf(
            LocalDate(2026, 10, 5) to 4_000L,
            LocalDate(2026, 10, 6) to 7_200L,
            LocalDate(2026, 10, 7) to 3_600L,
            LocalDate(2026, 10, 12) to 600L
        )
        val summary = StatsSummary(totalSeconds = byDay.values.sum(), sessions = 9, byDay = byDay)
        val recap = yearRecap(2026, summary, emptyList())!!
        assertEquals(15_400L, recap.totalSeconds)
        assertEquals(9, recap.sessions)
        assertEquals(4, recap.activeDays)
        assertEquals(3, recap.bestStreak)
        assertEquals(LocalDate(2026, 10, 6), recap.bestDay)
        assertEquals(7_200L, recap.bestDaySeconds)
        // Lunes: 4.600 s en total; martes: 7.200 s.
        assertEquals(DayOfWeek.TUESDAY, recap.bestWeekday)
    }
}
