package com.baltajmn.flowtime.features.screens.todoList

import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.goal.today
import kotlinx.datetime.LocalDate

data class TodoListState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = today(),
    val selectedDateToShow: String = today().toShowInList(),
    val today: LocalDate = today(),
    val tasks: List<Task> = emptyList(),
    /** La última borrada, mientras se puede deshacer. */
    val deleted: Task? = null,
    val message: TaskMessage? = null
)

enum class TaskMessage { EMPTY, LIMIT }
