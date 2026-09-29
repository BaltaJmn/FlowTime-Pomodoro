package com.baltajmn.flowtime.data.repository

import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.fakes.FakeSessionDao
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRepositoryTest {

    private val dao = FakeSessionDao()
    private val prefs = FakeDataProvider()
    private val sep29 = LocalDate.of(2026, 9, 29)

    private fun TestScope.repository() =
        DefaultSessionRepository(dao, prefs, scope = this, zone = { ZoneOffset.UTC })

    private fun at(day: LocalDate, hour: Long) =
        day.atStartOfDay(ZoneOffset.UTC).plusHours(hour).toInstant().toEpochMilli()

    @Test
    fun `el historial antiguo pasa a sesiones LEGACY con sus segundos`() = runTest {
        prefs.values["29092026"] = 25L
        prefs.values["30092026"] = 60L
        prefs.values["12345678"] = 5L // ocho cifras, pero no es una fecha
        prefs.values["THEME_COLOR"] = "Blue"

        repository().importLegacyOnce()
        advanceUntilIdle()

        assertEquals(
            mapOf("2026-09-29" to 25 * 60L, "2026-09-30" to 60 * 60L),
            dao.rows.associate { it.localDate to it.focusSeconds }
        )
        assertTrue(dao.rows.all { it.mode == SessionDb.MODE_LEGACY })
        assertEquals(true, prefs.values[SharedPreferencesItem.SESSIONS_IMPORTED.name])
    }

    @Test
    fun `no se importa dos veces`() = runTest {
        prefs.values["29092026"] = 25L

        repository().importLegacyOnce()
        advanceUntilIdle()
        repository().importLegacyOnce()
        advanceUntilIdle()

        assertEquals(1, dao.rows.size)
    }

    @Test
    fun `repetir una importacion que no llego a marcarse no duplica ni borra sesiones de verdad`() = runTest {
        prefs.values["29092026"] = 25L
        dao.insert(SessionDb(startedAt = 0, endedAt = 0, localDate = "2026-09-29", mode = "POMODORO", focusSeconds = 600))

        repository().importLegacyOnce()
        advanceUntilIdle()
        // El proceso murió antes de guardar que ya estaba hecho.
        prefs.values.remove(SharedPreferencesItem.SESSIONS_IMPORTED.name)
        repository().importLegacyOnce()
        advanceUntilIdle()

        assertEquals(listOf("POMODORO" to 600L, "LEGACY" to 25 * 60L), dao.rows.map { it.mode to it.focusSeconds })
    }

    @Test
    fun `una sesion guardada mientras se importa no se pierde`() = runTest {
        prefs.values["29092026"] = 25L
        val repository = repository()

        repository.importLegacyOnce()
        repository.record("POMODORO", at(sep29, 10), at(sep29, 10) + 1_200_000, focusSeconds = 1200)
        advanceUntilIdle()

        assertEquals(25 * 60L + 1200, repository.secondsByDay(sep29, sep29)[sep29])
    }

    @Test
    fun `leer espera a que termine la importacion`() = runTest {
        prefs.values["29092026"] = 25L
        val repository = repository()

        repository.importLegacyOnce()

        // Sin avanzar la prueba a mano: la consulta tiene que esperar a la importación.
        assertEquals(25 * 60L, repository.totalSeconds())
    }

    @Test
    fun `contar sesiones no cuenta el tiempo importado`() = runTest {
        prefs.values["29092026"] = 25L
        val repository = repository()

        repository.importLegacyOnce()
        repository.record("FLOW_TIME", at(sep29, 9), at(sep29, 9) + 1_800_000, focusSeconds = 1800)
        advanceUntilIdle()

        assertEquals(1, repository.countSessions(sep29, sep29))
    }
}
