package com.baltajmn.flowtime.features.screens.todoList.domain

import com.baltajmn.flowtime.core.common.model.TodoList
import com.baltajmn.flowtime.data.repository.TodoListRepository

interface GetTodoListByDateUseCase {
    suspend operator fun invoke(date: String): TodoList
}

class GetTodoListByDate(
    private val repository: TodoListRepository
) : GetTodoListByDateUseCase {

    override suspend fun invoke(date: String): TodoList {
        return repository.getTodoListByDate(date)
    }
}