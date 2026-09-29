package com.baltajmn.flowtime.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import com.baltajmn.flowtime.core.database.model.SessionDb
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private val db = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    ).build()
    private val dao = db.sessionDao()
    private val zone = ZoneId.of("Europe/Madrid")
    private val sep29 = LocalDate.of(2026, 9, 29)
    private val sep30 = LocalDate.of(2026, 9, 30)

    @After
    fun close() = db.close()

    private fun session(day: LocalDate, seconds: Long, mode: String = "POMODORO") =
        SessionDb(startedAt = 0, endedAt = 0, localDate = day.toString(), mode = mode, focusSeconds = seconds)

    @Test
    fun suma_los_segundos_de_cada_dia() = runTest {
        dao.insert(session(sep29, 24 * 60L + 59))
        dao.insert(session(sep29, 60))
        dao.insert(session(sep30, 30))

        assertEquals(mapOf("2026-09-29" to 25 * 60L + 59, "2026-09-30" to 30L), dao.secondsByDay("2026-09-01", "2026-09-30").associate { it.localDate to it.seconds })
        assertEquals(25 * 60L + 59, dao.secondsOn("2026-09-29").first())
        assertEquals(26 * 60L + 29, dao.totalSeconds())
        assertEquals(0L, dao.secondsOn("2026-10-01").first())
    }

    @Test
    fun anadir_a_un_dia_no_pasa_de_24_horas() = runTest {
        dao.insert(session(sep29, 23 * 3600L))

        dao.addToDays(mapOf(sep29 to 2 * 3600L, sep30 to 600L), replace = false, zone = zone)

        assertEquals(24 * 3600L, dao.secondsOn("2026-09-29").first())
        assertEquals(600L, dao.secondsOn("2026-09-30").first())
    }

    @Test
    fun sustituir_deja_el_dia_solo_con_lo_importado() = runTest {
        dao.insert(session(sep29, 3600))

        dao.addToDays(mapOf(sep29 to 1800L), replace = true, zone = zone)

        assertEquals(1800L, dao.secondsOn("2026-09-29").first())
    }

    @Test
    fun el_tiempo_sin_sesiones_empieza_a_medianoche_del_dia_en_la_zona_del_movil() {
        val legacy = SessionDb.legacy(sep29, 90, zone)

        assertEquals(sep29.atStartOfDay(zone).toInstant().toEpochMilli(), legacy.startedAt)
        assertEquals(legacy.startedAt + 90_000, legacy.endedAt)
        assertEquals(SessionDb.MODE_LEGACY, legacy.mode)
    }
}
