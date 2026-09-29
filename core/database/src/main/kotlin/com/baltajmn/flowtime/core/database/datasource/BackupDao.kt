package com.baltajmn.flowtime.core.database.datasource

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TagDb
import com.baltajmn.flowtime.core.database.model.TaskDb

/** Lo que sale y entra con la copia de seguridad a fichero. Importar solo añade: nunca borra nada. */
@Dao
abstract class BackupDao {

    @Query("SELECT * FROM session ORDER BY startedAt")
    abstract suspend fun sessions(): List<SessionDb>

    @Query("SELECT * FROM task ORDER BY plannedFor, position, id")
    abstract suspend fun tasks(): List<TaskDb>

    @Query("SELECT * FROM tag ORDER BY position, id")
    abstract suspend fun tags(): List<TagDb>

    @Insert
    protected abstract suspend fun insertTag(tag: TagDb): Long

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

    @Query("SELECT id FROM task WHERE createdAt = :createdAt LIMIT 1")
    protected abstract suspend fun taskCreatedAt(createdAt: Long): Long?

    @Insert
    protected abstract suspend fun insertTask(task: TaskDb): Long

    /**
     * Añade las etiquetas, las sesiones y las tareas que falten, todo o nada.
     *
     * - Una etiqueta ya está si se llama igual, sin contar mayúsculas. El `tagId` de las sesiones es
     *   el id de la etiqueta en [tags], y se cambia por el que tiene aquí.
     * - Las sesiones se comparan enteras (salvo la etiqueta) y contando las repetidas: un día puede
     *   tener varias LEGACY iguales (el mismo tiempo añadido dos veces desde un texto) y hay que
     *   traerlas todas.
     * - Una tarea ya está si se creó a la misma hora: si se editó después de la copia, se queda como
     *   está ahora. Su `id` y su `tagId` son los del fichero, igual que el `taskId` de las sesiones.
     */
    @Transaction
    open suspend fun restore(
        sessions: List<SessionDb>,
        tasks: List<TaskDb>,
        tags: List<TagDb> = emptyList()
    ): RestoreCount {
        val known = tags().toMutableList()
        var position = (known.maxOfOrNull { it.position } ?: -1) + 1
        var tagsAdded = 0
        val tagIds = tags.associate { incoming ->
            val match = known.firstOrNull { it.name.equals(incoming.name, ignoreCase = true) }
            val id = match?.id ?: insertTag(incoming.copy(id = 0, position = position++)).also { id ->
                known += incoming.copy(id = id)
                tagsAdded++
            }
            incoming.id to id
        }
        var tasksAdded = 0
        val taskIds = tasks.associate { task ->
            val id = taskCreatedAt(task.createdAt)
                ?: insertTask(task.copy(id = 0, tagId = task.tagId?.let(tagIds::get))).also { tasksAdded++ }
            task.id to id
        }
        var sessionsAdded = 0
        sessions
            .map { it.copy(id = 0, tagId = it.tagId?.let(tagIds::get), taskId = it.taskId?.let(taskIds::get)) }
            .groupBy { it.copy(tagId = null) }
            .forEach { (session, copies) ->
                val existing = session.run { countSame(startedAt, endedAt, mode, focusSeconds) }
                copies.drop(existing).forEach { insert(it) }
                sessionsAdded += (copies.size - existing).coerceAtLeast(0)
            }
        return RestoreCount(
            sessionsAdded = sessionsAdded,
            sessionsExisting = sessions.size - sessionsAdded,
            tasksAdded = tasksAdded,
            tagsAdded = tagsAdded
        )
    }
}

data class RestoreCount(
    val sessionsAdded: Int,
    val sessionsExisting: Int,
    val tasksAdded: Int,
    val tagsAdded: Int = 0
)
