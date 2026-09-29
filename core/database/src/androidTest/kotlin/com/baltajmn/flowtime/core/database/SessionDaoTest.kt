package com.baltajmn.flowtime.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import com.baltajmn.flowtime.core.database.model.ModeSeconds
import com.baltajmn.flowtime.core.database.model.PeriodTotals
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TaskDb
import com.baltajmn.flowtime.core.database.model.TaskTotal
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
    fun importar_el_historial_antiguo_solo_sustituye_el_tiempo_sin_sesiones() = runTest {
        dao.insert(session(sep29, 600))

        dao.importLegacy(mapOf(sep29 to 1500L), zone)
        dao.importLegacy(mapOf(sep29 to 1500L), zone)

        assertEquals(2100L, dao.secondsOn("2026-09-29").first())
    }

    @Test
    fun contar_sesiones_no_cuenta_el_tiempo_importado() = runTest {
        dao.insert(session(sep29, 600))
        dao.insert(session(sep29, 60, mode = SessionDb.MODE_LEGACY))
        dao.insert(session(sep30, 900))

        assertEquals(1, dao.countSessions("2026-09-01", "2026-09-29"))
        assertEquals(2, dao.countSessions("2026-09-01", "2026-09-30"))
    }

    @Test
    fun los_totales_de_un_periodo_suman_lo_importado_pero_no_como_sesion() = runTest {
        dao.insert(session(sep29, 1500))
        dao.insert(session(sep29, 600, mode = SessionDb.MODE_LEGACY))
        dao.insert(session(sep30, 900))

        assertEquals(
            PeriodTotals(totalSeconds = 2100, sessions = 1, sessionSeconds = 1500),
            dao.totals("2026-09-29", "2026-09-29")
        )
        assertEquals(PeriodTotals(0, 0, 0), dao.totals("2026-10-01", "2026-10-31"))
    }

    // strftime con 'localtime' usa la zona del móvil, la misma que ZoneId.systemDefault().
    private fun startingAt(day: LocalDate, hour: Int) =
        day.atTime(hour, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun por_hora_cuenta_la_hora_en_que_empezo_sin_el_tiempo_importado() = runTest {
        dao.insert(session(sep29, 600).copy(startedAt = startingAt(sep29, 9)))
        dao.insert(session(sep29, 300).copy(startedAt = startingAt(sep29, 9)))
        dao.insert(session(sep29, 1200).copy(startedAt = startingAt(sep29, 23)))
        dao.insert(SessionDb.legacy(sep29, 900, ZoneId.systemDefault()))

        assertEquals(
            mapOf(9 to 900L, 23 to 1200L),
            dao.secondsByHour("2026-09-29", "2026-09-29").associate { it.hour to it.seconds }
        )
    }

    @Test
    fun por_modo_con_su_numero_de_sesiones_sin_el_tiempo_importado() = runTest {
        dao.insert(session(sep29, 1500))
        dao.insert(session(sep29, 1500))
        dao.insert(session(sep30, 2400, mode = "FLOW_TIME"))
        dao.insert(session(sep30, 600, mode = SessionDb.MODE_LEGACY))

        assertEquals(
            listOf(ModeSeconds("FLOW_TIME", 2400, 1), ModeSeconds("POMODORO", 3000, 2)),
            dao.secondsByMode("2026-09-01", "2026-09-30").sortedBy { it.mode }
        )
    }

    @Test
    fun por_etiqueta_junta_las_sesiones_sin_etiqueta() = runTest {
        dao.insert(session(sep29, 600).copy(tagId = 1))
        dao.insert(session(sep29, 300).copy(tagId = 1))
        dao.insert(session(sep29, 900))
        dao.insert(session(sep29, 60, mode = SessionDb.MODE_LEGACY))

        assertEquals(
            mapOf(1L to 900L, null to 900L),
            dao.secondsByTag("2026-09-29", "2026-09-29").associate { it.tagId to it.seconds }
        )
    }

    private fun task(title: String) =
        TaskDb(title = title, description = "", plannedFor = sep29.toString(), createdAt = 0, position = 0)

    @Test
    fun las_tareas_con_mas_tiempo_llevan_su_titulo_y_las_borradas_no_salen() = runTest {
        val write = db.taskDao().insert(task("Escribir"))
        val read = db.taskDao().insert(task("Leer"))
        val deleted = 99L
        dao.insert(session(sep29, 600).copy(taskId = read))
        dao.insert(session(sep29, 1500).copy(taskId = write))
        dao.insert(session(sep30, 300).copy(taskId = read))
        dao.insert(session(sep30, 900).copy(taskId = deleted))
        dao.insert(session(sep30, 1200))

        assertEquals(
            listOf(TaskTotal(write, "Escribir", 1500), TaskTotal(read, "Leer", 900)),
            dao.topTasks("2026-09-01", "2026-09-30")
        )
        assertEquals(listOf(TaskTotal(read, "Leer", 300)), dao.topTasks("2026-09-30", "2026-09-30"))
        assertEquals(
            mapOf(write to 1500L, read to 900L, deleted to 900L),
            dao.secondsByTask().first().associate { it.taskId to it.seconds }
        )
    }

    @Test
    fun el_tiempo_sin_sesiones_empieza_a_medianoche_del_dia_en_la_zona_del_movil() {
        val legacy = SessionDb.legacy(sep29, 90, zone)

        assertEquals(sep29.atStartOfDay(zone).toInstant().toEpochMilli(), legacy.startedAt)
        assertEquals(legacy.startedAt + 90_000, legacy.endedAt)
        assertEquals(SessionDb.MODE_LEGACY, legacy.mode)
    }
}
