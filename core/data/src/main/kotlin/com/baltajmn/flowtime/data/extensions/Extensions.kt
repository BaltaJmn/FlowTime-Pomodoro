package com.baltajmn.flowtime.data.extensions

import com.baltajmn.flowtime.core.common.model.ListItem
import com.baltajmn.flowtime.core.common.model.TodoList
import com.baltajmn.flowtime.core.database.model.ListItemDb
import com.baltajmn.flowtime.core.database.model.TodoListDB

fun TodoListDB.toDomain() = TodoList(
    date = this.date,
    todoList = this.todoList.map {
        ListItem(
            id = it.id,
            title = it.title,
            description = it.description,
            done = it.done
        )
    }
)

fun TodoList.toDB() = TodoListDB(
    date = this.date,
    todoList = this.todoList.map {
        ListItemDb(
            id = it.id,
            title = it.title,
            description = it.description,
            done = it.done
        )
    }
)