package com.baltajmn.flowtime.features.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.KEEP_SCREEN_ON
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.NOTIFICATIONS_EXPLAINED
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusSnapshot
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.data.timer.actionsFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class TimerUiState(
    val mode: TimerMode,
    val phase: Phase = Phase.IDLE,
    val paused: Boolean = false,
    val time: String = "00:00",
    val minutesToday: String = "",
    /** El objetivo de hoy, ya formateado. Vacío hasta que se lee. */
    val goalToday: String = "",
    val continueAfterBreak: Boolean = true,
    val keepScreenOn: Boolean = true
) {
    val isActive get() = phase != Phase.IDLE
    val actions get() = actionsFor(mode, phase, paused)
}

/** Una pantalla por modo, todas sobre el mismo motor: solo una sesión puede estar en marcha. */
class TimerViewModel(
    private val mode: TimerMode,
    private val engine: FocusEngine,
    private val dataProvider: DataProvider,
    goals: GoalRepository
) : ViewModel() {

    private val continueAfterBreak =
        MutableStateFlow(dataProvider.getCheckValue(mode.continueAfterBreakKey))
    private val keepScreenOn = dataProvider.getBoolean(KEEP_SCREEN_ON, true)

    // El día sale del repositorio del objetivo: con la pantalla abierta pasada la medianoche, antes
    // seguía sumando los minutos de ayer.
    val uiState: StateFlow<TimerUiState> =
        combine(
            engine.snapshots(),
            continueAfterBreak,
            goals.today.onStart<DayProgress?> { emit(null) },
            ::toUiState
        ).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            toUiState(engine.snapshot(), continueAfterBreak.value, today = null)
        )

    fun onAction(action: TimerAction) = engine.perform(action, mode)

    /** El permiso de notificaciones se explica una sola vez: después, el aviso queda en Ajustes. */
    fun explainNotificationsOnce(): Boolean {
        if (dataProvider.getBoolean(NOTIFICATIONS_EXPLAINED, false)) return false
        dataProvider.setBoolean(NOTIFICATIONS_EXPLAINED, true)
        return true
    }

    fun changeSwitch(value: Boolean) {
        dataProvider.setCheckValue(mode.continueAfterBreakKey, value)
        continueAfterBreak.value = value
    }

    private fun toUiState(
        snapshot: FocusSnapshot,
        continueAfter: Boolean,
        today: DayProgress?
    ): TimerUiState {
        val session = snapshot.state
        // Si la sesión en marcha es de otro modo, esta pantalla se ve parada.
        val mine = session.isActive && session.mode == mode
        return TimerUiState(
            mode = mode,
            phase = if (mine) session.phase else Phase.IDLE,
            paused = mine && session.isPaused,
            time = (if (mine) snapshot.displaySeconds else engine.workMillis(mode) / 1000).formatSecondsToTime(),
            minutesToday = ((today?.seconds ?: 0) / 60).formatMinutesStudying(),
            goalToday = today?.goalMinutes?.toLong()?.formatMinutesStudying().orEmpty(),
            continueAfterBreak = continueAfter,
            keepScreenOn = keepScreenOn
        )
    }
}
