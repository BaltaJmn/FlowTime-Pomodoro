package com.baltajmn.flowtime.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import com.baltajmn.flowtime.core.database.model.HourSeconds
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TaskDb
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant

/** Room en el iPhone, con su SQLite: las consultas y las transacciones de SessionDaoTest que más arriesgan. */
class IosDatabaseTest {

    private val db = Room.inMemoryDatabaseBuilder<AppDatabase>().setDriver(BundledSQLiteDriver()).build()
    private val sessions = db.sessionDao()
    private val sep29 = LocalDate(2026, 9, 29)

    @AfterTest
    fun close() = db.close()

    private fun session(startedAt: Long, seconds: Long, taskId: Long? = null) = SessionDb(
        startedAt = startedAt,
        endedAt = startedAt + seconds * 1000,
        localDate = sep29.toString(),
        mode = "POMODORO",
        focusSeconds = seconds,
        taskId = taskId
    )

    @Test
    fun addingToADayStopsAtTwentyFourHours() = runTest {
        sessions.insert(session(0, 23 * 3600L))

        sessions.addToDays(mapOf(sep29 to 2 * 3600L), replace = false, zone = TimeZone.of("Europe/Madrid"))

        assertEquals(24 * 3600L, sessions.secondsOn("2026-09-29").first())
    }

    // strftime con 'localtime' tiene que usar la misma zona que el resto de la app.
    @Test
    fun theHourIsTheLocalOne() = runTest {
        val tenThirty = sep29.atTime(10, 30).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        sessions.insert(session(tenThirty, 1500))

        assertEquals(listOf(HourSeconds(10, 1500)), sessions.secondsByHour("2026-09-29", "2026-09-29"))
    }

    @Test
    fun theTimeOfEachTaskFollowsItsSessions() = runTest {
        val task = db.taskDao().insert(
            TaskDb(title = "Leer", description = "", plannedFor = "2026-09-29", createdAt = 0, position = 0)
        )
        sessions.insert(session(0, 600, taskId = task))
        sessions.insert(session(1_000_000, 300, taskId = task))

        assertEquals(900L, sessions.secondsByTask().first().single().seconds)
    }

    @Test
    fun theDatabaseOnDiskOpens() = runTest {
        val disk = appDatabase()

        assertEquals(0L, disk.sessionDao().secondsOn("1999-01-01").first())
        disk.close()
    }
}
