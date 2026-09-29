package com.baltajmn.flowtime.core.database.datasource

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TodoListDB

/** Lo que sale y entra con la copia de seguridad a fichero. Importar solo añade: nunca borra nada. */
@Dao
abstract class BackupDao {

    @Query("SELECT * FROM session ORDER BY startedAt")
    abstract suspend fun sessions(): List<SessionDb>

    @Query("SELECT * FROM todoList ORDER BY date")
    abstract suspend fun todoLists(): List<TodoListDB>

    @Insert
    protected abstract suspend fun insert(session: SessionDb)

    @Query(
        "SELECT COUNT(*) FROM session WHERE startedAt = :startedAt AND endedAt = :endedAt " +
            "AND mode = :mode AND focusSeconds = :focusSeconds"
    )
    protected abstract suspend fun countSame(
        startedAt: Long,
        endedAt: Long,
        mode: String,
        focusSeconds: Long
    ): Int

    @Query("SELECT * FROM todoList WHERE date = :date")
    protected abstract suspend fun todoList(date: String): TodoListDB?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun putTodoList(todoList: TodoListDB)

    /**
     * Añade las sesiones y las tareas que falten, todo o nada. Las sesiones se comparan enteras y
     * contando las repetidas: un día puede tener varias LEGACY iguales (el mismo tiempo añadido dos
     * veces desde un texto) y hay que traerlas todas. Una tarea ya está si tiene el mismo id (la hora
     * en que se creó): si se editó después de la copia, se queda como está ahora.
     */
    @Transaction
    open suspend fun restore(sessions: List<SessionDb>, todoLists: List<TodoListDB>): RestoreCount {
        var sessionsAdded = 0
        sessions.groupingBy { it.copy(id = 0) }.eachCount().forEach { (session, copies) ->
            val missing = copies - session.run { countSame(startedAt, endedAt, mode, focusSeconds) }
            repeat(missing) { insert(session) }
            sessionsAdded += missing.coerceAtLeast(0)
        }
        var tasksAdded = 0
        todoLists.forEach { incoming ->
            val current = todoList(incoming.date)?.todoList.orEmpty()
            val missing = incoming.todoList.filter { item -> current.none { it.id == item.id } }
            if (missing.isNotEmpty()) {
                putTodoList(TodoListDB(date = incoming.date, todoList = current + missing))
                tasksAdded += missing.size
            }
        }
        return RestoreCount(
            sessionsAdded = sessionsAdded,
            sessionsExisting = sessions.size - sessionsAdded,
            tasksAdded = tasksAdded
        )
    }
}

data class RestoreCount(val sessionsAdded: Int, val sessionsExisting: Int, val tasksAdded: Int)
