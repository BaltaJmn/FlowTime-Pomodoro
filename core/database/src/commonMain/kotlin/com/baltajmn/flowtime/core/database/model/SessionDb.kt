package com.baltajmn.flowtime.core.database.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

/**
 * Un bloque de trabajo terminado, con sus segundos reales. [MODE_LEGACY] es tiempo sin sesiones que
 * lo expliquen: el historial de antes, que solo guardaba minutos por día, o un texto importado.
 */
@Entity(tableName = "session", indices = [Index("localDate"), Index("tagId"), Index("taskId")])
data class SessionDb(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    /** yyyy-MM-dd del día en que empezó, en la zona del móvil. Una sesión cuenta entera para ese día. */
    val localDate: String,
    val mode: String,
    val focusSeconds: Long,
    /** La etiqueta del bloque, o null si no tenía. */
    val tagId: Long? = null,
    /** La tarea en la que se trabajó, o null. */
    val taskId: Long? = null
) {
    companion object {
        const val MODE_LEGACY = "LEGACY"
        const val DAY_SECONDS = 24 * 60 * 60L

        fun legacy(day: LocalDate, seconds: Long, zone: TimeZone): SessionDb {
            val start = day.atStartOfDayIn(zone).toEpochMilliseconds()
            return SessionDb(
                startedAt = start,
                endedAt = start + seconds * 1000,
                localDate = day.toString(),
                mode = MODE_LEGACY,
                focusSeconds = seconds
            )
        }
    }
}

data class DaySeconds(val localDate: String, val seconds: Long)

/** Una sesión con el título de su tarea, si tiene y no se ha borrado. */
data class SessionRow(
    val id: Long,
    val startedAt: Long,
    val endedAt: Long,
    val localDate: String,
    val mode: String,
    val focusSeconds: Long,
    val tagId: Long?,
    val taskId: Long?,
    val taskTitle: String?
)
