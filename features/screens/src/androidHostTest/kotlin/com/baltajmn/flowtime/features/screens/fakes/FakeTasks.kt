package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.task.TaskRepository
import com.baltajmn.flowtime.data.task.TaskResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/** Tareas en memoria, sin límite, con las mismas reglas de qué se ve cada día. */
class FakeTasks(vararg tasks: Task) : TaskRepository {

    val tasks = MutableStateFlow(tasks.toList())

    override fun day(day: LocalDate, today: LocalDate): Flow<List<Task>> = tasks.map { list ->
        list.filter { it.showsOn(day, today) }.sortedWith(ORDER)
    }

    override suspend fun add(
        title: String,
        description: String,
        plannedFor: LocalDate,
        tagId: Long?
    ): TaskResult {
        val id = (tasks.value.maxOfOrNull { it.id } ?: 0) + 1
        tasks.update { it + Task(id, title, description, plannedFor, tagId = tagId) }
        return TaskResult.Done(id)
    }

    override suspend fun edit(id: Long, title: String, description: String): TaskResult {
        change(id) { copy(title = title, description = description) }
        return TaskResult.Done(id)
    }

    override suspend fun setDone(id: Long, done: Boolean, today: LocalDate) =
        change(id) { copy(doneOn = today.takeIf { done }) }

    override suspend fun delete(id: Long): Task? {
        val task = tasks.value.firstOrNull { it.id == id }
        tasks.update { list -> list.filterNot { it.id == id } }
        return task
    }

    override suspend fun restore(task: Task) = tasks.update { it + task }

    override suspend fun moveTo(id: Long, day: LocalDate) = change(id) { copy(plannedFor = day) }

    override suspend fun reorder(ids: List<Long>) = Unit

    private fun change(id: Long, edit: Task.() -> Task) =
        tasks.update { list -> list.map { if (it.id == id) it.edit() else it } }

    private companion object {
        val ORDER = compareBy(Task::plannedFor, Task::position)
    }
}
