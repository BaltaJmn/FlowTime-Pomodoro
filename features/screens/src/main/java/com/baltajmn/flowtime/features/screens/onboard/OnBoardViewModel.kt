package com.baltajmn.flowtime.features.screens.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ON_BOARD
import com.baltajmn.flowtime.data.goal.DailyGoal
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.TimerMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnBoardUiState(
    val mode: TimerMode,
    val goalMinutes: Int,
    /** Con una sesión en marcha el modo no se cambia: se enseña el suyo. */
    val modeLocked: Boolean = false
)

/**
 * La introducción: qué es FlowTime, el modo y el objetivo, y empezar. La primera vez propone FlowTime
 * y el objetivo por defecto; abierta desde Ajustes, parte de lo que ya hay y no borra nada.
 */
class OnBoardViewModel(
    private val dataProvider: DataProvider,
    private val engine: FocusEngine,
    private val goals: GoalRepository
) : ViewModel() {

    private val firstRun = dataProvider.getCheckValue(SHOW_ON_BOARD)

    private val initial = OnBoardUiState(
        mode = if (firstRun) TimerMode.FLOW_TIME else engine.state.value.mode,
        goalMinutes = goals.currentGoal,
        modeLocked = engine.state.value.isActive
    )

    private val _uiState = MutableStateFlow(initial)
    val uiState: StateFlow<OnBoardUiState> = _uiState.asStateFlow()

    private val _event = Channel<Event>()
    val event = _event.receiveAsFlow()

    fun selectMode(mode: TimerMode) = _uiState.update {
        if (it.modeLocked) it else it.copy(mode = mode)
    }

    fun changeGoal(delta: Int) = _uiState.update {
        val minutes = it.goalMinutes + delta
        it.copy(goalMinutes = minutes.coerceIn(DailyGoal.MIN_MINUTES, DailyGoal.MAX_MINUTES))
    }

    /** "Empezar": con lo elegido y, la primera vez, directo al temporizador de ese modo. */
    fun start() = finish(_uiState.value, openTimer = true)

    /** "Saltar" termina de verdad y deja lo de antes: la primera vez, FlowTime y el objetivo por defecto. */
    fun skip() = finish(initial, openTimer = false)

    private fun finish(choice: OnBoardUiState, openTimer: Boolean) {
        engine.select(choice.mode)
        if (choice.goalMinutes != goals.currentGoal) goals.setGoal(choice.goalMinutes)
        dataProvider.setCheckValue(SHOW_ON_BOARD, false)
        val event = when {
            !firstRun -> Event.Back
            openTimer -> Event.NavigateToMainGraph(timer = choice.mode)
            else -> Event.NavigateToMainGraph(timer = null)
        }
        viewModelScope.launch { _event.send(event) }
    }

    sealed interface Event {
        /** La primera vez: a la app y, si se ha pulsado "Empezar", al temporizador de [timer]. */
        data class NavigateToMainGraph(val timer: TimerMode?) : Event

        /** Abierta desde Ajustes: vuelve allí. */
        data object Back : Event
    }
}
