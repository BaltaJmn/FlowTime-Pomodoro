package com.baltajmn.flowtime.data.task

import com.baltajmn.flowtime.core.database.datasource.TaskDao
import com.baltajmn.flowtime.core.database.model.TaskDb
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.data.pro.ProGate
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class Task(
    val id: Long,
    val title: String,
    val description: String = "",
    /** El día para el que se apuntó. */
    val plannedFor: LocalDate,
    /** El día en que se completó, o null si está pendiente. */
    val doneOn: LocalDate? = null,
    val createdAt: Long = 0,
    val position: Int = 0,
    val tagId: Long? = null
) {
    val done: Boolean get() = doneOn != null

    /**
     * Si se ve en [day]: la apuntada o la completada ese día y, hoy, también las pendientes de días
     * anteriores. Un día pasado no enseña las pendientes que arrastraba: esas se ven hoy.
     */
    fun showsOn(day: LocalDate, today: LocalDate) =
        plannedFor == day || doneOn == day || (day == today && doneOn == null && plannedFor < today)

    /** Los días que lleva pendiente desde el día para el que se apuntó, vista en [day]. */
    fun daysLate(day: LocalDate): Long =
        if (doneOn == null && plannedFor < day) ChronoUnit.DAYS.between(plannedFor, day) else 0
}

sealed interface TaskResult {
    data class Done(val id: Long) : TaskResult

    /** Sin título. */
    data object Invalid : TaskResult

    /** Sin Pro, ya hay [Limits.FREE_PENDING_TASKS] pendientes. */
    data object LimitReached : TaskResult
}

/**
 * Las tareas, cada una con su día. Sin Pro caben [Limits.FREE_PENDING_TASKS] pendientes a la vez:
 * con ellas se puede hacer de todo menos añadir otra, y nunca se bloquea ni se borra ninguna.
 */
interface TaskRepository {
    /** Las que se ven en [day], empezando por las que vienen de días anteriores. */
    fun day(day: LocalDate, today: LocalDate): Flow<List<Task>>

    suspend fun add(title: String, description: String, plannedFor: LocalDate, tagId: Long? = null): TaskResult

    suspend fun edit(id: Long, title: String, description: String): TaskResult

    suspend fun setDone(id: Long, done: Boolean, today: LocalDate)

    /** La borrada, para poder deshacerlo. */
    suspend fun delete(id: Long): Task?

    /** Deshace un borrado: vuelve con su id y todo lo que tenía. */
    suspend fun restore(task: Task)

    suspend fun moveTo(id: Long, day: LocalDate)

    /** Las de un día, en el orden de [ids]. */
    suspend fun reorder(ids: List<Long>)
}

class DefaultTaskRepository(
    private val dao: TaskDao,
    private val gate: ProGate,
    private val clock: () -> Long = System::currentTimeMillis
) : TaskRepository {

    override fun day(day: LocalDate, today: LocalDate): Flow<List<Task>> =
        dao.around(day.toString(), today.toString()).map { rows ->
            rows.mapNotNull { it.toTask() }.filter { it.showsOn(day, today) }
        }

    override suspend fun add(
        title: String,
        description: String,
        plannedFor: LocalDate,
        tagId: Long?
    ): TaskResult {
        val clean = title.trim().takeIf { it.isNotEmpty() } ?: return TaskResult.Invalid
        if (!gate.allowsOneMore(dao.countPending(), Limits.FREE_PENDING_TASKS)) return TaskResult.LimitReached
        val day = plannedFor.toString()
        val id = dao.insert(
            TaskDb(
                title = clean,
                description = description.trim(),
                plannedFor = day,
                createdAt = clock(),
                position = dao.nextPosition(day),
                tagId = tagId
            )
        )
        return TaskResult.Done(id)
    }

    override suspend fun edit(id: Long, title: String, description: String): TaskResult {
        val clean = title.trim().takeIf { it.isNotEmpty() } ?: return TaskResult.Invalid
        val task = dao.get(id) ?: return TaskResult.Done(id)
        dao.update(task.copy(title = clean, description = description.trim()))
        return TaskResult.Done(id)
    }

    override suspend fun setDone(id: Long, done: Boolean, today: LocalDate) {
        val task = dao.get(id) ?: return
        dao.update(task.copy(doneOn = if (done) today.toString() else null))
    }

    override suspend fun delete(id: Long): Task? {
        val task = dao.get(id) ?: return null
        dao.delete(id)
        return task.toTask()
    }

    override suspend fun restore(task: Task) {
        dao.insert(task.toDb())
    }

    override suspend fun moveTo(id: Long, day: LocalDate) {
        val task = dao.get(id) ?: return
        val key = day.toString()
        dao.update(task.copy(plannedFor = key, position = dao.nextPosition(key)))
    }

    override suspend fun reorder(ids: List<Long>) = dao.reorder(ids)

    // Un día que no se entiende (no debería haberlo) no puede tirar la lista entera.
    private fun TaskDb.toTask(): Task? = try {
        Task(
            id = id,
            title = title,
            description = description,
            plannedFor = LocalDate.parse(plannedFor),
            doneOn = doneOn?.let(LocalDate::parse),
            createdAt = createdAt,
            position = position,
            tagId = tagId
        )
    } catch (e: DateTimeParseException) {
        null
    }

    private fun Task.toDb() = TaskDb(
        id = id,
        title = title,
        description = description,
        plannedFor = plannedFor.toString(),
        doneOn = doneOn?.toString(),
        createdAt = createdAt,
        position = position,
        tagId = tagId
    )
}
