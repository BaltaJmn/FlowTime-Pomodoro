package com.baltajmn.flowtime.features.screens.todoList

import com.baltajmn.flowtime.data.task.Task
import java.time.LocalDate

data class TodoListState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateToShow: String = LocalDate.now().toShowInList(),
    val today: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    /** La última borrada, mientras se puede deshacer. */
    val deleted: Task? = null,
    val message: TaskMessage? = null
)

enum class TaskMessage { EMPTY, LIMIT }
