package com.baltajmn.flowtime.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Una tarea. Los días son yyyy-MM-dd y no horas, como [SessionDb.localDate]: así no hay líos de zona
 * horaria. Una pendiente de un día pasado se sigue viendo hoy hasta que se completa.
 */
@Entity(tableName = "task", indices = [Index("plannedFor"), Index("doneOn")])
data class TaskDb(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    /** El día para el que se apuntó. */
    val plannedFor: String,
    /** El día en que se completó, o null si está pendiente. */
    val doneOn: String? = null,
    val createdAt: Long,
    val position: Int,
    val tagId: Long? = null
)
