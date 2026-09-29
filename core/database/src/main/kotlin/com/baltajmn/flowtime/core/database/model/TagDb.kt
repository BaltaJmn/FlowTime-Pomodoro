package com.baltajmn.flowtime.core.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Una etiqueta para las sesiones. Archivarla no borra nada: sus sesiones la siguen teniendo. */
@Entity(tableName = "tag")
data class TagDb(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Índice en la paleta fija de colores, que tiene su versión clara y oscura. */
    val color: Int,
    val position: Int,
    val archived: Boolean = false,
    val createdAt: Long
)
