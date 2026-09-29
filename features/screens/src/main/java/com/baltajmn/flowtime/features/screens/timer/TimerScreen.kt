package com.baltajmn.flowtime.features.screens.timer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.screen.TimerBaseScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TimerScreen(
    mode: TimerMode,
    viewModel: TimerViewModel = koinViewModel(key = mode.name) { parametersOf(mode) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    KeepScreenOn(active = state.keepScreenOn && state.isActive && !state.paused)

    TimerBaseScreen(
        state = state,
        title = title(state),
        onAction = viewModel::onAction,
        onSwitchChanged = viewModel::changeSwitch
    )
}

@Composable
private fun title(state: TimerUiState): String = stringResource(
    when {
        state.paused -> R.string.time_title_paused
        state.phase == Phase.WORK -> R.string.time_title_working
        state.phase == Phase.BREAK -> R.string.time_title_resting
        state.mode == TimerMode.POMODORO -> R.string.pomodoro_title
        state.mode == TimerMode.FLOW_TIME -> R.string.flow_time_title
        else -> R.string.percentage_title
    }
)

/** Solo mientras cuenta y esta pantalla está a la vista: ya no hace falta para no perder el tiempo. */
@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}
