package com.baltajmn.flowtime.features.screens.percentage

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.ComposableLifecycle
import com.baltajmn.flowtime.features.screens.common.PercentageState
import com.baltajmn.flowtime.features.screens.common.composable.screen.AnimatedTimerContent
import com.baltajmn.flowtime.features.screens.common.composable.screen.TimerBaseScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun PercentageScreen(
    viewModel: PercentageViewModel = koinViewModel(),
    listState: LazyListState,
    onTimerRunning: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ComposableLifecycle { _, event ->
        if (event == Lifecycle.Event.ON_START) {
            viewModel.getPercentageConfig()
            viewModel.getCurrentMinutes()
        }
    }

    onTimerRunning(state.isTimerRunning || state.isBreakRunning)

    AnimatedTimerContent(state, listState) { timerState ->
        TimerBaseScreen(
            state = timerState,
            listState = listState,
            titleProvider = { s: PercentageState, ctx ->
                when {
                    s.isTimerRunning && !s.isBreakRunning -> ctx.getString(R.string.time_title_working)
                    !s.isTimerRunning && s.isBreakRunning -> ctx.getString(R.string.time_title_resting)
                    else -> ctx.getString(R.string.percentage_title)
                }
            },
            onStartClick = { viewModel.startTimer() },
            onBreakClick = { viewModel.continueWithBreak() },
            onFinishClick = { viewModel.stopTimer() },
            onSwitchChanged = {
                viewModel.changeSwitch(
                    value = it
                )
            }
        )
    }
}