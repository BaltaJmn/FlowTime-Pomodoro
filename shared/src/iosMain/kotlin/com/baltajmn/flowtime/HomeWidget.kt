package com.baltajmn.flowtime

import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.goal_today
import com.baltajmn.flowtime.core.design.resources.streak_days
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import kotlin.time.Clock
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString

/** El widget de la pantalla de inicio, que también es de Swift (FocusWidget.swift). */
interface HomeWidget {
    /**
     * [status]: el modo, y con sesión cómo va. [date] y [time], como en LiveActivity.show; sin sesión,
     * [time] es lo que dura el trabajo del modo. [primary]: START, RESUME o PAUSE, y [stopLabel], con
     * sesión. [goal]: "1 h 25 min de 2 h", con [goalFraction].
     */
    fun update(
        status: String,
        streak: String?,
        countsDown: Boolean,
        date: Double?,
        time: String,
        primary: String,
        primaryLabel: String,
        stopLabel: String?,
        goal: String?,
        goalFraction: Float
    )
}

/**
 * Lo mismo que el widget de Android: el modo y cómo va, la racha, el reloj, empezar, pausar o seguir
 * y parar, y el objetivo de hoy. Se actualiza con cada cambio de la sesión y del progreso de hoy.
 */
internal class HomeWidgetUpdater(private val engine: FocusEngine, private val widget: HomeWidget) {
    suspend fun update(state: FocusState, today: DayProgress?, streak: Streak?) {
        val status = listOfNotNull(
            getString(state.mode.label),
            state.takeIf { it.isActive }?.let { getString(it.title) }
        ).joinToString(" · ")
        val days = streak?.current ?: 0
        val streakText = if (days > 0) getPluralString(Res.plurals.streak_days, days, days) else null
        val primary = when {
            !state.isActive -> TimerAction.START
            state.isPaused -> TimerAction.RESUME
            else -> TimerAction.PAUSE
        }
        val primaryLabel = getString(primary.label)
        val stopLabel = if (state.isActive) getString(TimerAction.STOP.label) else null
        val goal = today?.let {
            getString(
                Res.string.goal_today,
                (it.seconds / 60).formatMinutesStudying(),
                it.goalMinutes.toLong().formatMinutesStudying()
            )
        }
        // Sin suspender desde aquí: el reloj, con la hora de ahora.
        val snapshot = engine.snapshot(state)
        val now = Clock.System.now().toEpochMilliseconds()
        val date = when {
            !state.running -> null
            state.countsDown -> now + snapshot.remainingMillis
            else -> now - snapshot.elapsedMillis
        }
        val seconds = if (state.isActive) snapshot.displaySeconds else engine.workMillis(state.mode) / 1000
        widget.update(
            status = status,
            streak = streakText,
            countsDown = state.countsDown,
            date = date?.let { it / 1000.0 },
            time = seconds.formatSecondsToTime(),
            primary = primary.name,
            primaryLabel = primaryLabel,
            stopLabel = stopLabel,
            goal = goal,
            goalFraction = today?.fraction ?: 0f
        )
    }
}
