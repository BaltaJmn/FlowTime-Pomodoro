package com.baltajmn.flowtime.features.screens.todoList.domain

import com.baltajmn.flowtime.core.common.model.TodoList
import com.baltajmn.flowtime.data.repository.TodoListRepository

interface InsertTodoListUseCase {
    suspend operator fun invoke(todoList: TodoList)
}

class InsertTodoList(
    private val repository: TodoListRepository
) : InsertTodoListUseCase {

    override suspend fun invoke(todoList: TodoList) {
        repository.insertTodoList(todoList)
    }
}