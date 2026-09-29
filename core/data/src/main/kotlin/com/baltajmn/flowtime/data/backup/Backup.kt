package com.baltajmn.flowtime.data.backup

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

/**
 * El fichero de la copia de seguridad. Todo con valores por defecto: un fichero de una versión
 * anterior, al que le falte algo, se lee igual. Uno de una versión posterior ([format] mayor) se
 * rechaza, porque podría traer datos que esta versión perdería al importarlo. [app] y [format] son
 * obligatorios: sin ellos, cualquier JSON (un "{}") pasaría por una copia vacía.
 */
@Serializable
data class Backup(
    @Required val app: String = APP,
    @Required val format: Int = FORMAT,
    val exportedAt: Long = 0,
    val appVersion: String = "",
    val sessions: List<BackupSession> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val settings: BackupSettings? = null
) {
    companion object {
        const val APP = "flowtime"
        const val FORMAT = 1
    }
}

@Serializable
data class BackupSession(
    val startedAt: Long,
    val endedAt: Long,
    /** yyyy-MM-dd del día en que empezó. */
    val localDate: String,
    val mode: String,
    val focusSeconds: Long
)

/** Con la forma que tendrán las tareas en su propia tabla (#39): así el formato no cambia entonces. */
@Serializable
data class BackupTask(
    val title: String,
    val description: String = "",
    /** yyyy-MM-dd del día para el que se apuntó. */
    val plannedFor: String,
    /** yyyy-MM-dd del día en que se completó, o null si está pendiente. */
    val doneOn: String? = null,
    val createdAt: Long,
    val position: Int = 0
)

@Serializable
data class BackupSettings(
    val theme: String? = null,
    val darkMode: String? = null,
    val dynamicColor: Boolean? = null,
    val pomodoro: BackupRange? = null,
    val flowTime: List<BackupRange>? = null,
    val percentage: Long? = null,
    /** Por el nombre del modo: POMODORO, FLOW_TIME o PERCENTAGE. */
    val continueAfterBreak: Map<String, Boolean> = emptyMap(),
    val showAlert: Boolean? = null,
    val keepScreenOn: Boolean? = null,
    val showSound: Boolean? = null,
    /** Por el nombre del sonido, de 0 a 1. */
    val soundVolumes: Map<String, Float> = emptyMap()
)

@Serializable
data class BackupRange(val totalRange: Int, val endRange: Int, val rest: Int)
