package com.baltajmn.flowtime.features.screens.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.KEEP_SCREEN_ON
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.NOTIFICATIONS_EXPLAINED
import com.baltajmn.flowtime.data.repository.SessionRepository
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
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TimerUiState(
    val mode: TimerMode,
    val phase: Phase = Phase.IDLE,
    val paused: Boolean = false,
    val time: String = "00:00",
    val minutesToday: String = "",
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
    sessions: SessionRepository
) : ViewModel() {

    private val continueAfterBreak =
        MutableStateFlow(dataProvider.getCheckValue(mode.continueAfterBreakKey))
    private val keepScreenOn = dataProvider.getBoolean(KEEP_SCREEN_ON, true)

    val uiState: StateFlow<TimerUiState> =
        combine(
            engine.snapshots(),
            continueAfterBreak,
            sessions.secondsOn(LocalDate.now()),
            ::toUiState
        ).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            toUiState(engine.snapshot(), continueAfterBreak.value, secondsToday = 0)
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

    private fun toUiState(snapshot: FocusSnapshot, continueAfter: Boolean, secondsToday: Long): TimerUiState {
        val session = snapshot.state
        // Si la sesión en marcha es de otro modo, esta pantalla se ve parada.
        val mine = session.isActive && session.mode == mode
        return TimerUiState(
            mode = mode,
            phase = if (mine) session.phase else Phase.IDLE,
            paused = mine && session.isPaused,
            time = (if (mine) snapshot.displaySeconds else engine.workMillis(mode) / 1000).formatSecondsToTime(),
            minutesToday = (secondsToday / 60).formatMinutesStudying(),
            continueAfterBreak = continueAfter,
            keepScreenOn = keepScreenOn
        )
    }
}
