package com.baltajmn.flowtime.features.screens.todoList

import com.baltajmn.flowtime.core.common.extensions.toShowInList
import com.baltajmn.flowtime.core.common.model.TodoList
import java.time.LocalDate

data class TodoListState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateToShow: String = LocalDate.now().toShowInList(),
    val currentTodoList: TodoList = TodoList()
)