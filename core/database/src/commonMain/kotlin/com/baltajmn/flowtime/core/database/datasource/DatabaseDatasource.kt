package com.baltajmn.flowtime.core.database.datasource

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.Transaction
import com.baltajmn.flowtime.core.database.model.DaySeconds
import com.baltajmn.flowtime.core.database.model.HourSeconds
import com.baltajmn.flowtime.core.database.model.ModeSeconds
import com.baltajmn.flowtime.core.database.model.PeriodTotals
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.SessionDb.Companion.DAY_SECONDS
import com.baltajmn.flowtime.core.database.model.TagDb
import com.baltajmn.flowtime.core.database.model.TagSeconds
import com.baltajmn.flowtime.core.database.model.TaskSeconds
import com.baltajmn.flowtime.core.database.model.TaskTotal
import com.baltajmn.flowtime.core.database.model.TaskDb
import com.baltajmn.flowtime.core.database.model.TodoListDB
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

// De la 1 a la 2 y de la 3 a la 4, a mano y solo en Android (Migrations.kt): el iPhone empieza en la 5.
// De la 2 a la 3, automática: una tabla nueva (tag) y una columna que admite nulos (session.tagId).
// De la 3 a la 4, las tareas pasan de un JSON por día a una fila cada una.
// De la 4 a la 5, automática: session.taskId (#40).
// todoList se queda, sin usar, hasta la versión de la app siguiente a la que publique la 4: si la
// migración de las tareas tuviera un fallo, de ahí se podrían volver a leer.
@Database(
    entities = [TodoListDB::class, SessionDb::class, TagDb::class, TaskDb::class],
    version = 5,
    autoMigrations = [AutoMigration(from = 2, to = 3), AutoMigration(from = 4, to = 5)]
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun backupDao(): BackupDao
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
}

// Room escribe los `actual` de cada plataforma.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

@Dao
abstract class SessionDao {
    @Insert
    abstract suspend fun insert(session: SessionDb)

    @Query(
        "SELECT localDate, SUM(focusSeconds) AS seconds FROM session " +
            "WHERE localDate BETWEEN :from AND :to GROUP BY localDate"
    )
    abstract suspend fun secondsByDay(from: String, to: String): List<DaySeconds>

    @Query("SELECT COALESCE(SUM(focusSeconds), 0) FROM session WHERE localDate = :day")
    abstract fun secondsOn(day: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(focusSeconds), 0) FROM session")
    abstract suspend fun totalSeconds(): Long

    /** Sesiones de trabajo de verdad: el tiempo importado no cuenta como sesión. */
    @Query(
        "SELECT COUNT(*) FROM session " +
            "WHERE mode != '${SessionDb.MODE_LEGACY}' AND localDate BETWEEN :from AND :to"
    )
    abstract suspend fun countSessions(from: String, to: String): Int

    @Query(
        "SELECT COALESCE(SUM(focusSeconds), 0) AS totalSeconds, " +
            "COALESCE(SUM(CASE WHEN mode != '${SessionDb.MODE_LEGACY}' THEN 1 ELSE 0 END), 0) AS sessions, " +
            "COALESCE(SUM(CASE WHEN mode != '${SessionDb.MODE_LEGACY}' THEN focusSeconds ELSE 0 END), 0) " +
            "AS sessionSeconds FROM session WHERE localDate BETWEEN :from AND :to"
    )
    abstract suspend fun totals(from: String, to: String): PeriodTotals

    /**
     * Por la hora en que empezó cada sesión, como cuenta para el día en que empezó. Sin LEGACY, que
     * tiene la hora puesta a medianoche.
     */
    @Query(
        "SELECT CAST(strftime('%H', startedAt / 1000, 'unixepoch', 'localtime') AS INTEGER) AS hour, " +
            "SUM(focusSeconds) AS seconds FROM session " +
            "WHERE mode != '${SessionDb.MODE_LEGACY}' AND localDate BETWEEN :from AND :to GROUP BY hour"
    )
    abstract suspend fun secondsByHour(from: String, to: String): List<HourSeconds>

    @Query(
        "SELECT mode, SUM(focusSeconds) AS seconds, COUNT(*) AS sessions FROM session " +
            "WHERE mode != '${SessionDb.MODE_LEGACY}' AND localDate BETWEEN :from AND :to GROUP BY mode"
    )
    abstract suspend fun secondsByMode(from: String, to: String): List<ModeSeconds>

    @Query(
        "SELECT tagId, SUM(focusSeconds) AS seconds FROM session " +
            "WHERE mode != '${SessionDb.MODE_LEGACY}' AND localDate BETWEEN :from AND :to GROUP BY tagId"
    )
    abstract suspend fun secondsByTag(from: String, to: String): List<TagSeconds>

    @Query("SELECT * FROM session ORDER BY startedAt")
    abstract suspend fun all(): List<SessionDb>

    /** El tiempo de cada tarea, que se actualiza con cada sesión nueva. */
    @Query(
        "SELECT taskId, SUM(focusSeconds) AS seconds FROM session " +
            "WHERE taskId IS NOT NULL GROUP BY taskId"
    )
    abstract fun secondsByTask(): Flow<List<TaskSeconds>>

    /** Las tareas con más tiempo en un periodo, con su título. Las borradas no salen. */
    @Query(
        "SELECT session.taskId AS taskId, task.title AS title, SUM(session.focusSeconds) AS seconds " +
            "FROM session JOIN task ON task.id = session.taskId " +
            "WHERE session.localDate BETWEEN :from AND :to GROUP BY session.taskId ORDER BY seconds DESC"
    )
    abstract suspend fun topTasks(from: String, to: String): List<TaskTotal>

    @Query("SELECT COALESCE(SUM(focusSeconds), 0) FROM session WHERE localDate = :day")
    protected abstract suspend fun secondsOnce(day: String): Long

    @Query("DELETE FROM session WHERE localDate = :day")
    protected abstract suspend fun deleteDay(day: String)

    @Query("DELETE FROM session WHERE localDate = :day AND mode = '${SessionDb.MODE_LEGACY}'")
    protected abstract suspend fun deleteLegacyDay(day: String)

    /**
     * El historial de antes de las sesiones, como tiempo [SessionDb.MODE_LEGACY] de cada día. Solo
     * sustituye ese tiempo: repetirlo no duplica nada ni borra las sesiones de verdad del mismo día.
     */
    @Transaction
    open suspend fun importLegacy(secondsByDay: Map<LocalDate, Long>, zone: TimeZone) {
        secondsByDay.forEach { (day, seconds) ->
            val key = day.toString()
            deleteLegacyDay(key)
            val add = seconds.coerceAtMost(DAY_SECONDS - secondsOnce(key))
            if (add > 0) insert(SessionDb.legacy(day, add, zone))
        }
    }

    /**
     * Añade tiempo a cada día como una sesión [SessionDb.MODE_LEGACY], sin pasar de las 24 horas.
     * Con [replace], el día se queda solo con ese tiempo.
     */
    @Transaction
    open suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean, zone: TimeZone) {
        secondsByDay.forEach { (day, seconds) ->
            val key = day.toString()
            if (replace) deleteDay(key)
            val add = seconds.coerceAtMost(DAY_SECONDS - secondsOnce(key))
            if (add > 0) insert(SessionDb.legacy(day, add, zone))
        }
    }
}
