package com.baltajmn.flowtime.core.database.datasource

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.baltajmn.flowtime.core.database.converter.ItemConverter
import com.baltajmn.flowtime.core.database.model.DaySeconds
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.SessionDb.Companion.DAY_SECONDS
import com.baltajmn.flowtime.core.database.model.TodoListDB
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [TodoListDB::class, SessionDb::class],
    version = 2
)
@TypeConverters(ItemConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoListDao(): TodoListDao
    abstract fun sessionDao(): SessionDao
}

/** Escrita a mano: con la migración destructiva, un fallo borraba todas las tareas sin avisar. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `session` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `startedAt` INTEGER NOT NULL,
                `endedAt` INTEGER NOT NULL,
                `localDate` TEXT NOT NULL,
                `mode` TEXT NOT NULL,
                `focusSeconds` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_session_localDate` ON `session` (`localDate`)")
    }
}

@Dao
interface TodoListDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodoList(todoListDB: TodoListDB)

    @Query("SELECT * FROM todoList WHERE date = :date")
    suspend fun getTodoListByDate(date: String): TodoListDB?

    @Update
    suspend fun updateTodoList(todoListDB: TodoListDB)
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
    open suspend fun importLegacy(secondsByDay: Map<LocalDate, Long>, zone: ZoneId) {
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
    open suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean, zone: ZoneId) {
        secondsByDay.forEach { (day, seconds) ->
            val key = day.toString()
            if (replace) deleteDay(key)
            val add = seconds.coerceAtMost(DAY_SECONDS - secondsOnce(key))
            if (add > 0) insert(SessionDb.legacy(day, add, zone))
        }
    }
}
