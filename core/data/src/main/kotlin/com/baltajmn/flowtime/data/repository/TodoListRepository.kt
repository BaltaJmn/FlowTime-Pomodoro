package com.baltajmn.flowtime.data.repository

import com.baltajmn.flowtime.core.common.model.TodoList
import com.baltajmn.flowtime.core.database.datasource.TodoListDao
import com.baltajmn.flowtime.data.extensions.toDB
import com.baltajmn.flowtime.data.extensions.toDomain

interface TodoListRepository {
    suspend fun getTodoListByDate(date: String): TodoList
    suspend fun insertTodoList(todoList: TodoList)
    suspend fun updateTodoList(todoList: TodoList)
}

class DefaultTodoListRepository(
    private val todoListDao: TodoListDao
) : TodoListRepository {

    override suspend fun getTodoListByDate(date: String): TodoList {
        return todoListDao.getTodoListByDate(date)?.toDomain() ?: TodoList.empty().copy(date = date)
    }

    override suspend fun insertTodoList(todoList: TodoList) {
        todoListDao.insertTodoList(todoList.toDB())
    }

    override suspend fun updateTodoList(todoList: TodoList) {
        todoListDao.updateTodoList(todoList.toDB())
    }
}