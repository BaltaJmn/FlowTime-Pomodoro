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
    val tags: List<BackupTag> = emptyList(),
    val tasks: List<BackupTask> = emptyList(),
    val settings: BackupSettings? = null
) {
    companion object {
        const val APP = "flowtime"

        /** 2: etiquetas (#37) y tareas con su etiqueta (#39). Una versión que solo lee el 1 las perdería. */
        const val FORMAT = 2
    }
}

@Serializable
data class BackupSession(
    val startedAt: Long,
    val endedAt: Long,
    /** yyyy-MM-dd del día en que empezó. */
    val localDate: String,
    val mode: String,
    val focusSeconds: Long,
    /** El [BackupTag.id] de su etiqueta dentro de este fichero. */
    val tagId: Long? = null
)

/** El [id] solo vale dentro del fichero: al importar, cada etiqueta se busca por el nombre. */
@Serializable
data class BackupTag(
    val id: Long,
    val name: String,
    val color: Int = 0,
    val position: Int = 0,
    val archived: Boolean = false,
    val createdAt: Long = 0
)

@Serializable
data class BackupTask(
    val title: String,
    val description: String = "",
    /** yyyy-MM-dd del día para el que se apuntó. */
    val plannedFor: String,
    /** yyyy-MM-dd del día en que se completó, o null si está pendiente. */
    val doneOn: String? = null,
    val createdAt: Long,
    val position: Int = 0,
    /** El [BackupTag.id] de su etiqueta dentro de este fichero. */
    val tagId: Long? = null
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
    val soundVolumes: Map<String, Float> = emptyMap(),
    /** El objetivo diario con su historial: cada día se mide con el que tenía entonces. */
    val dailyGoal: List<BackupGoalChange>? = null
)

@Serializable
data class BackupGoalChange(
    /** yyyy-MM-dd del día desde el que vale. */
    val from: String,
    val minutes: Int
)

@Serializable
data class BackupRange(val totalRange: Int, val endRange: Int, val rest: Int)
