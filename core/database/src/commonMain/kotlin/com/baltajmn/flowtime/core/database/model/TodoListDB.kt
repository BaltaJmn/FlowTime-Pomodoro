package com.baltajmn.flowtime.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Las tareas de antes de la versión 4 de la base de datos, un JSON por día. Ya no se usa: se queda
 * para que la tabla siga ahí (ver AppDatabase), y la migración la lee con SQL.
 */
@Entity(tableName = "todoList")
data class TodoListDB(
    @PrimaryKey val date: String,
    val todoList: String
)
