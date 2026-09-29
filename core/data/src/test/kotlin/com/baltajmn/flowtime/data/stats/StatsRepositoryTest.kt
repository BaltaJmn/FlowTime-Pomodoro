package com.baltajmn.flowtime.data.stats

import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.data.fakes.FakeSessionDao
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsRepositoryTest {

    private val dao = FakeSessionDao()
    private val stats = DefaultStatsRepository(dao, zone = { ZoneOffset.UTC })

    // Martes 29 de septiembre de 2026.
    private val today = LocalDate.of(2026, 9, 29)
    private val week = StatsPeriod(PeriodKind.WEEK)

    private fun session(day: LocalDate, hour: Int, minutes: Long, mode: String = "POMODORO", tagId: Long? = null) {
        val start = day.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        dao.rows += SessionDb(0, start, start + minutes * 60_000, day.toString(), mode, minutes * 60, tagId)
    }

    @Test
    fun `los periodos van de lunes a domingo, de dia 1 a fin de mes y de enero a diciembre`() {
        assertEquals(LocalDate.of(2026, 9, 28)..LocalDate.of(2026, 10, 4), week.range(today))
        assertEquals(LocalDate.of(2026, 9, 21)..LocalDate.of(2026, 9, 27), week.previous.range(today))
        assertEquals(LocalDate.of(2026, 8, 1)..LocalDate.of(2026, 8, 31), StatsPeriod(PeriodKind.MONTH, 1).range(today))
        assertEquals(LocalDate.of(2026, 1, 1)..LocalDate.of(2026, 12, 31), StatsPeriod(PeriodKind.YEAR).range(today))
        assertEquals(today..today, StatsPeriod(PeriodKind.DAY).range(today))
    }

    @Test
    fun `totales y medias de la semana`() = runTest {
        session(today.minusDays(1), 9, 50)
        session(today.minusDays(1), 16, 25, mode = "FLOW_TIME")
        session(today, 10, 45)

        val summary = stats.summary(week, today)

        assertEquals(120 * 60L, summary.totalSeconds)
        assertEquals(3, summary.sessions)
        assertEquals(40 * 60L, summary.averageSessionSeconds)
        // Solo lunes y martes: los días que faltan de la semana no bajan la media.
        assertEquals(60 * 60L, summary.dailyAverageSeconds)
        assertEquals(today.minusDays(1), summary.bestDay)
        assertEquals(75 * 60L, summary.bestDaySeconds)
    }

    @Test
    fun `el tiempo importado suma pero no es sesion, ni tiene hora ni modo`() = runTest {
        dao.rows += SessionDb.legacy(today, 2 * 3600, ZoneOffset.UTC)
        session(today, 9, 30)

        val summary = stats.summary(week, today)

        assertEquals(150 * 60L, summary.totalSeconds)
        assertEquals(1, summary.sessions)
        assertEquals(30 * 60L, summary.averageSessionSeconds)
        assertEquals(30 * 60L, stats.byHour(week, today)[9])
        assertEquals(0L, stats.byHour(week, today)[0])
        assertEquals(mapOf("POMODORO" to 30 * 60L), stats.byMode(week, today))
    }

    @Test
    fun `la comparacion con el periodo anterior`() = runTest {
        session(today.minusWeeks(1), 9, 100)
        session(today, 9, 112)

        assertEquals(0.12f, stats.change(week, today)!!, 0.001f)
        // Sin nada antes no hay con qué comparar.
        assertNull(stats.change(week.previous, today))
    }

    @Test
    fun `un periodo sin datos`() = runTest {
        assertEquals(StatsSummary(), stats.summary(week, today))
        assertEquals(List(24) { 0L }, stats.byHour(week, today))
    }

    @Test
    fun `por etiqueta, con las que no tienen juntas`() = runTest {
        session(today, 9, 30, tagId = 1)
        session(today, 11, 20, tagId = 1)
        session(today, 15, 10)

        assertEquals(mapOf(1L to 50 * 60L, null to 10 * 60L), stats.byTag(week, today))
    }

    @Test
    fun `el CSV lleva cada sesion con su etiqueta y escapa las comas`() = runTest {
        session(today, 9, 25, tagId = 1)

        val csv = stats.csv(tagNames = mapOf(1L to "Estudio, tema 4"))

        assertEquals(
            "startedAt,endedAt,date,mode,tag,task,focusMinutes\n" +
                "2026-09-29 09:00,2026-09-29 09:25,2026-09-29,POMODORO,\"Estudio, tema 4\",,25.0\n",
            csv
        )
    }
}
