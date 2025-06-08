package com.baltajmn.flowtime.features.screens.common.composable.screen

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.baltajmn.flowtime.features.screens.common.TimerState

@Composable
fun <T : TimerState<T>> TimerBaseScreen(
    state: T,
    titleProvider: (T, Context) -> String,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
    onBreakClick: (() -> Unit)? = null,
    onSwitchChanged: (Boolean) -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        TimerLandscapeContent(
            state = state,
            titleProvider = titleProvider,
            onStartClick = onStartClick,
            onFinishClick = onFinishClick,
            onBreakClick = onBreakClick,
            onSwitchChanged = onSwitchChanged
        )
    } else {
        TimerPortraitContent(
            state = state,
            titleProvider = titleProvider,
            onStartClick = onStartClick,
            onFinishClick = onFinishClick,
            onBreakClick = onBreakClick,
            onSwitchChanged = onSwitchChanged
        )
    }
}
