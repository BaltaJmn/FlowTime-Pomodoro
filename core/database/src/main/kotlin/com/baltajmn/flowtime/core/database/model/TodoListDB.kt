package com.baltajmn.flowtime.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todoList")
data class TodoListDB(
    @PrimaryKey val date: String,
    val todoList: List<ListItemDb>
)

data class ListItemDb(
    val id: Int,
    val title: String,
    val description: String,
    val done: Boolean
)