package com.baltajmn.flowtime.features.screens.common.composable.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.features.screens.common.TimerState

@Composable
fun <T : TimerState<T>> AnimatedTimerContent(
    state: T,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) { isLoading ->
        if (isLoading) {
            LoadingView()
        } else {
            content(state)
        }
    }
}