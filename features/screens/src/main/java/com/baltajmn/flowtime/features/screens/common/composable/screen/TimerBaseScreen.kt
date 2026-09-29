package com.baltajmn.flowtime.features.screens.common.composable.screen

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.timer.TimerUiState

@Composable
fun TimerBaseScreen(
    state: TimerUiState,
    title: String,
    onAction: (TimerAction) -> Unit,
    onSwitchChanged: (Boolean) -> Unit,
    onTagSelected: (Long?) -> Unit,
    onTaskSelected: (Task?) -> Unit,
    onTaskDone: () -> Unit,
    onTaskNotYet: () -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        TimerLandscapeContent(
            state,
            title,
            onAction,
            onSwitchChanged,
            onTagSelected,
            onTaskSelected,
            onTaskDone,
            onTaskNotYet
        )
    } else {
        TimerPortraitContent(
            state,
            title,
            onAction,
            onSwitchChanged,
            onTagSelected,
            onTaskSelected,
            onTaskDone,
            onTaskNotYet
        )
    }
}
