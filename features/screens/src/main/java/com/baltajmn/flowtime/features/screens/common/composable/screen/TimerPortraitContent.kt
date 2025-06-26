package com.baltajmn.flowtime.features.screens.common.composable.screen

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.features.screens.common.PomodoroState
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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(128.dp))

        ScreenTitle(text = titleProvider(state, context))

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                TimeContent(secondsFormatted = state.secondsFormatted)
                Spacer(modifier = Modifier.height(16.dp))
                MinutesStudying(minutesStudying = state.minutesStudying)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        ButtonsContent(
            state = state,
            onStartClick = onStartClick,
            onFinishClick = onFinishClick,
            onBreakClick = onBreakClick
        )

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = context.getString(R.string.pomodoro_continue_after_break),
            style = Title.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Switch(
            checked = state.continueAfterBreak,
            onCheckedChange = onSwitchChanged,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PomodoroPortraitContentPreview() {
    TimerPortraitContent(
        state = PomodoroState(
            secondsFormatted = "00:00",
            minutesStudying = 0.toString(),
            continueAfterBreak = false
        ),
        titleProvider = { _, _ -> "Pomodoro" },
        onStartClick = {},
        onFinishClick = {},
        onSwitchChanged = {}
    )
}