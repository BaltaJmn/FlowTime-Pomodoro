package com.baltajmn.flowtime.features.screens.todoList.domain

import com.baltajmn.flowtime.core.common.model.TodoList
import com.baltajmn.flowtime.data.repository.TodoListRepository

interface UpdateTodoListUseCase {
    suspend operator fun invoke(todoList: TodoList)
}

class UpdateTodoList(
    private val repository: TodoListRepository
) : UpdateTodoListUseCase {

    override suspend fun invoke(todoList: TodoList) {
        repository.updateTodoList(todoList)
    }
}