package com.baltajmn.flowtime.data.backup

import com.baltajmn.flowtime.core.database.datasource.BackupDao
import com.baltajmn.flowtime.core.database.model.ListItemDb
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TodoListDB
import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.sound.PlayerType
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.core.design.theme.DarkMode
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PERCENTAGE_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ALERT
import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.fakes.FakeSessionRepository
import com.baltajmn.flowtime.data.goal.GoalChange
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.timer.TimerMode
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRepositoryTest {

    /** Las dos tablas en memoria: lo justo para que funcione la transacción de la clase base. */
    private class FakeBackupDao : BackupDao() {
        val rows = mutableListOf<SessionDb>()
        val lists = sortedMapOf<String, TodoListDB>()
        private var nextId = 1L

        override suspend fun sessions() = rows.sortedBy { it.startedAt }

        override suspend fun todoLists() = lists.values.toList()

        override suspend fun insert(session: SessionDb) {
            rows += session.copy(id = nextId++)
        }

        override suspend fun countSame(
            startedAt: Long,
            endedAt: Long,
            mode: String,
            focusSeconds: Long
        ) = rows.count {
            it.startedAt == startedAt && it.endedAt == endedAt && it.mode == mode &&
                it.focusSeconds == focusSeconds
        }

        override suspend fun todoList(date: String) = lists[date]

        override suspend fun putTodoList(todoList: TodoListDB) {
            lists[todoList.date] = todoList
        }
    }

    /** Una instalación de la app: su base de datos, sus preferencias y el repositorio sobre ellas. */
    private class Device {
        val dao = FakeBackupDao()
        val prefs = FakeDataProvider()
        val appearance = AppearanceRepository(prefs)
        // De verdad no se puede: el mezclador abre la salida de audio de Android.
        val ambience = mockk<Ambience>(relaxed = true)
        val goals = GoalRepository(prefs, FakeSessionRepository())
        val backups = DefaultBackupRepository(
            dao = dao,
            dataProvider = prefs,
            appearance = appearance,
            ambience = ambience,
            goals = goals,
            appVersion = "3.0",
            clock = { NOW }
        )

        val sessions get() = dao.rows.map { it.copy(id = 0) }.sortedBy { it.startedAt }

        suspend fun backup() = (backups.read(backups.export()) as BackupRead.Valid).backup
    }

    private fun session(
        day: String,
        hour: Int,
        mode: String = "POMODORO",
        minutes: Long = 25
    ): SessionDb {
        val start = LocalDate.parse(day).atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return SessionDb(
            startedAt = start,
            endedAt = start + minutes * 60_000,
            localDate = day,
            mode = mode,
            focusSeconds = minutes * 60
        )
    }

    private fun task(id: Long, title: String, done: Boolean = false) =
        ListItemDb(id, title, description = "", done)

    @Test
    fun `una copia se restaura entera en otro movil`() = runTest {
        val old = Device().apply {
            dao.rows += session("2026-09-29", 10, mode = "FLOW_TIME", minutes = 50)
            dao.rows += session("2026-09-28", 9)
            dao.rows += SessionDb.legacy(LocalDate.of(2025, 1, 5), 3600, ZoneOffset.UTC)
            dao.lists["2026-09-29"] = TodoListDB(
                "2026-09-29",
                listOf(task(2, "Leer"), ListItemDb(1, "Repasar", "Tema 4", done = true))
            )
            appearance.setTheme(AppTheme.Green)
            appearance.setDarkMode(DarkMode.DARK)
            prefs.setObject(POMODORO_RANGE, RangeModel(totalRange = 50, endRange = 50, rest = 10))
            prefs.setObject(
                FLOW_TIME_RANGE,
                mutableListOf(RangeModel(25, 25, 5), RangeModel(60, 35, 9))
            )
            prefs.setLong(PERCENTAGE_RANGE, 30)
            prefs.setCheckValue(TimerMode.POMODORO.continueAfterBreakKey, false)
            prefs.setBoolean(SHOW_ALERT, false)
            prefs.setFloat(PlayerType.RAIN.name, 0.8f)
            goals.setGoal(45, LocalDate.of(2026, 1, 10))
            goals.setGoal(90, LocalDate.of(2026, 9, 1))
        }
        val new = Device()

        val backup = old.backup()
        val result = new.backups.restore(backup, withSettings = true)

        assertEquals(RestoreResult(sessionsAdded = 3, sessionsExisting = 0, tasksAdded = 2), result)
        assertEquals(old.sessions, new.sessions)
        assertEquals(old.dao.lists, new.dao.lists)
        assertEquals("3.0", backup.appVersion)
        assertEquals(NOW, backup.exportedAt)
        with(new) {
            assertEquals(AppTheme.Green, appearance.appearance.value.theme)
            assertEquals(DarkMode.DARK, appearance.appearance.value.darkMode)
            assertEquals(RangeModel(50, 50, 10), prefs.getRangeModel(POMODORO_RANGE))
            assertEquals(
                listOf(RangeModel(25, 25, 5), RangeModel(60, 35, 9)),
                prefs.getRangeModelList(FLOW_TIME_RANGE)
            )
            assertEquals(30L, prefs.getLong(PERCENTAGE_RANGE))
            assertFalse(prefs.getCheckValue(TimerMode.POMODORO.continueAfterBreakKey))
            assertTrue(prefs.getCheckValue(TimerMode.FLOW_TIME.continueAfterBreakKey))
            assertFalse(prefs.getBoolean(SHOW_ALERT, true))
            assertEquals(
                listOf(GoalChange(LocalDate.of(2026, 1, 10), 45), GoalChange(LocalDate.of(2026, 9, 1), 90)),
                goals.history.value
            )
            // Solo el volumen que se tocó, y por Ambience: el panel no tiene que esperar a reiniciar.
            verify(exactly = 1) { ambience.setVolume(any(), any()) }
            verify { ambience.setVolume(PlayerType.RAIN, 0.8f) }
        }
    }

    @Test
    fun `importar dos veces no duplica nada`() = runTest {
        val old = Device().apply {
            dao.rows += session("2026-09-28", 9)
            dao.lists["2026-09-28"] = TodoListDB("2026-09-28", listOf(task(1, "Leer")))
        }
        val new = Device()

        new.backups.restore(old.backup(), withSettings = false)
        val again = new.backups.restore(old.backup(), withSettings = false)

        assertEquals(RestoreResult(sessionsAdded = 0, sessionsExisting = 1, tasksAdded = 0), again)
        assertEquals(old.sessions, new.sessions)
        assertEquals(old.dao.lists, new.dao.lists)
    }

    @Test
    fun `las sesiones iguales de un dia se traen todas`() = runTest {
        // El mismo tiempo añadido dos veces desde un texto: dos filas idénticas, y cuentan las dos.
        val legacy = SessionDb.legacy(LocalDate.of(2026, 3, 1), 1800, ZoneOffset.UTC)
        val old = Device().apply { dao.rows += listOf(legacy, legacy) }
        val new = Device().apply { dao.rows += legacy }

        val result = new.backups.restore(old.backup(), withSettings = false)

        assertEquals(RestoreResult(sessionsAdded = 1, sessionsExisting = 1, tasksAdded = 0), result)
        assertEquals(old.sessions, new.sessions)
    }

    @Test
    fun `las tareas que ya estan se quedan como estan y las que faltan van al final`() = runTest {
        val day = "2026-09-29"
        val old = Device().apply {
            dao.lists[day] = TodoListDB(day, listOf(task(1, "Repasar"), task(2, "Leer")))
        }
        // La 1 se editó después de la copia; la 3 es nueva.
        val new = Device().apply {
            dao.lists[day] = TodoListDB(
                day,
                listOf(task(1, "Repasar el tema 4", done = true), task(3, "Correr"))
            )
        }

        val result = new.backups.restore(old.backup(), withSettings = false)

        assertEquals(1, result.tasksAdded)
        assertEquals(
            listOf(task(1, "Repasar el tema 4", done = true), task(3, "Correr"), task(2, "Leer")),
            new.dao.lists.getValue(day).todoList
        )
    }

    @Test
    fun `los ajustes solo se importan si se piden`() = runTest {
        val old = Device().apply {
            appearance.setTheme(AppTheme.Pink)
            prefs.setBoolean(SHOW_ALERT, false)
            prefs.setFloat(PlayerType.FIRE.name, 0.1f)
            goals.setGoal(120, LocalDate.of(2026, 9, 1))
        }
        val new = Device()

        new.backups.restore(old.backup(), withSettings = false)

        assertEquals(AppTheme.Blue, new.appearance.appearance.value.theme)
        assertTrue(new.prefs.getBoolean(SHOW_ALERT, true))
        verify(exactly = 0) { new.ambience.setVolume(any(), any()) }
        assertEquals(emptyList<GoalChange>(), new.goals.history.value)
    }

    @Test
    fun `lo que no es una copia de esta app no se lee`() = runTest {
        val backups = Device().backups
        val file = Device().apply { dao.rows += session("2026-09-28", 9) }.backups.export()

        assertEquals(BackupRead.TooNew, backups.read("""{"app":"flowtime","format":2}"""))
        assertEquals(BackupRead.NotABackup, backups.read("{}"))
        assertEquals(BackupRead.NotABackup, backups.read("""{"app":"otra","format":1}"""))
        assertEquals(BackupRead.NotABackup, backups.read("hola"))
        assertEquals(BackupRead.NotABackup, backups.read(file.dropLast(40)))
        // Un campo que esta versión no conoce, de una versión posterior con el mismo formato, sí.
        val newer = backups.read("""{"app":"flowtime","format":1,"streak":12}""")
        assertTrue(newer is BackupRead.Valid)
    }

    @Test
    fun `las filas sin sentido se saltan`() = runTest {
        fun row(
            localDate: String = "2026-02-28",
            mode: String = "POMODORO",
            seconds: Long = 60,
            endedAt: Long = 60_000
        ) = BackupSession(startedAt = 0, endedAt, localDate, mode, seconds)
        val backup = Backup(
            sessions = listOf(
                row(localDate = "2026-02-30"),
                row(seconds = 90_000),
                row(endedAt = -1),
                row(mode = ""),
                row()
            ),
            tasks = listOf(
                BackupTask(title = " ", plannedFor = "2026-02-28", createdAt = 1),
                BackupTask(title = "Leer", plannedFor = "ayer", createdAt = 2),
                BackupTask(title = "Correr", plannedFor = "2026-02-28", createdAt = 3)
            ),
            settings = BackupSettings(
                theme = "Rainbow",
                pomodoro = BackupRange(totalRange = 45, endRange = 0, rest = 15),
                percentage = 500,
                soundVolumes = mapOf("RAIN" to 3f, "VACUUM" to 0.3f)
            )
        )
        val device = Device()

        val result = device.backups.restore(backup, withSettings = true)

        assertEquals(RestoreResult(sessionsAdded = 1, sessionsExisting = 0, tasksAdded = 1), result)
        assertEquals(AppTheme.Blue, device.appearance.appearance.value.theme)
        assertNull(device.prefs.getRangeModel(POMODORO_RANGE))
        assertEquals(100L, device.prefs.getLong(PERCENTAGE_RANGE))
        verify(exactly = 1) { device.ambience.setVolume(any(), any()) }
        verify { device.ambience.setVolume(PlayerType.RAIN, 1f) }
    }

    @Test
    fun `la fecha de la ultima copia se guarda cuando el fichero se ha escrito`() = runTest {
        val device = Device()

        device.backups.export()
        assertNull(device.backups.lastExportAt)

        device.backups.markExported()
        assertEquals(NOW, device.backups.lastExportAt)
    }

    private companion object {
        const val NOW = 1_790_000_000_000L
    }
}
