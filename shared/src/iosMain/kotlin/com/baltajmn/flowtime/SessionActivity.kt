package com.baltajmn.flowtime

import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.time_title_paused
import com.baltajmn.flowtime.core.design.resources.time_title_resting
import com.baltajmn.flowtime.core.design.resources.time_title_working
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import kotlin.time.Clock
import org.jetbrains.compose.resources.getString

/**
 * La Live Activity de la sesión, en la pantalla bloqueada y en la Dynamic Island. ActivityKit solo
 * existe en Swift, así que la pide y la dibuja iosApp (FocusActivities.swift y FlowTimeWidgets).
 */
interface LiveActivity {
    /**
     * [date], en segundos desde 1970: en marcha, cuándo acaba la fase si cuenta hacia atrás o cuándo
     * empezó si cuenta hacia delante; en pausa, null, y se enseña [time].
     */
    fun show(title: String, subtitle: String, task: String?, countsDown: Boolean, date: Double?, time: String)

    fun end()
}

/**
 * Lo mismo que la notificación de Android mientras dura la sesión: trabajando, descansando o en
 * pausa; el modo y la etiqueta; el reloj y la tarea. El reloj lo mueve el sistema, así que solo se
 * actualiza cuando cambia la sesión.
 */
internal class SessionActivity(
    private val engine: FocusEngine,
    private val tags: TagRepository,
    private val activity: LiveActivity
) {
    suspend fun update(state: FocusState) {
        if (!state.isActive) return activity.end()
        val title = getString(state.title)
        val mode = getString(state.mode.label)
        // ponytail: con la app suspendida nadie la actualiza, así que al acabar la fase se queda en
        // 0:00 hasta abrir la app (el aviso de fin de fase sí llega). Seguir sola pediría un servidor
        // de notificaciones push.
        val snapshot = engine.snapshot(state)
        val now = Clock.System.now().toEpochMilliseconds()
        val date = when {
            !state.running -> null
            state.countsDown -> now + snapshot.remainingMillis
            else -> now - snapshot.elapsedMillis
        }
        val tag = state.tagId?.let { id -> tags.all.value.firstOrNull { it.id == id } }
        activity.show(
            title = title,
            subtitle = listOfNotNull(mode, tag?.name).joinToString(" · "),
            task = state.taskTitle,
            countsDown = state.countsDown,
            date = date?.let { it / 1000.0 },
            time = snapshot.displaySeconds.formatSecondsToTime()
        )
    }
}

/** Trabajando, descansando o en pausa, como en la notificación de Android. */
private val FocusState.title
    get() = when {
        isPaused -> Res.string.time_title_paused
        phase == Phase.BREAK -> Res.string.time_title_resting
        else -> Res.string.time_title_working
    }
