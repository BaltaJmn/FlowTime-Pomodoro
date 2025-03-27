package com.baltajmn.flowtime.features.screens.common.composable.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.features.screens.common.TimerState
import com.baltajmn.flowtime.features.screens.common.composable.components.ButtonsContent
import com.baltajmn.flowtime.features.screens.common.composable.components.MinutesStudying
import com.baltajmn.flowtime.features.screens.common.composable.components.ScreenTitle
import com.baltajmn.flowtime.features.screens.common.composable.components.TimeContent

@Composable
fun <T : TimerState<T>> TimerPortraitContent(
    state: T,
    titleProvider: (T, Context) -> String,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
    onBreakClick: (() -> Unit)? = null,
    onSwitchChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 120.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenTitle(text = titleProvider(state, context))
        Spacer(modifier = Modifier.height(32.dp))
        TimeContent(secondsFormatted = state.secondsFormatted)
        Spacer(modifier = Modifier.height(16.dp))
        MinutesStudying(minutesStudying = state.minutesStudying)
        Spacer(modifier = Modifier.height(64.dp))
        ButtonsContent(
            state = state,
            onStartClick = onStartClick,
            onFinishClick = onFinishClick,
            onBreakClick = onBreakClick
        )
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = context.getString(com.baltajmn.flowtime.core.design.R.string.pomodoro_continue_after_break),
            style = Title.copy(fontSize = 12.sp, color = MaterialTheme.colorScheme.tertiary)
        )
        Switch(
            checked = state.continueAfterBreak,
            onCheckedChange = onSwitchChanged,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                uncheckedTrackColor = MaterialTheme.colorScheme.secondary
            )
        )
    }
}
