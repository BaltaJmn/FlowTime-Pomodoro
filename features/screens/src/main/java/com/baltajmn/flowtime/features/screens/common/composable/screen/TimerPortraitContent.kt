package com.baltajmn.flowtime.features.screens.common.composable.screen

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
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.components.ButtonsContent
import com.baltajmn.flowtime.features.screens.common.composable.components.MinutesStudying
import com.baltajmn.flowtime.features.screens.common.composable.components.ScreenTitle
import com.baltajmn.flowtime.features.screens.common.composable.components.TagChips
import com.baltajmn.flowtime.features.screens.common.composable.components.TimeContent
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.timer.TimerUiState

@Composable
fun TimerPortraitContent(
    state: TimerUiState,
    title: String,
    onAction: (TimerAction) -> Unit,
    onSwitchChanged: (Boolean) -> Unit,
    onTagSelected: (Long?) -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(128.dp))

        ScreenTitle(text = title)

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
                TimeContent(secondsFormatted = state.time)
                Spacer(modifier = Modifier.height(16.dp))
                MinutesStudying(minutesStudying = state.minutesToday, goal = state.goalToday)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        TagChips(tags = state.tags, selected = state.tagId, onSelect = onTagSelected)

        Spacer(modifier = Modifier.height(48.dp))

        ButtonsContent(state = state, onAction = onAction)

        Spacer(modifier = Modifier.height(64.dp))

        Text(
            text = stringResource(R.string.pomodoro_continue_after_break),
            style = Title.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Switch(
            checked = state.continueAfterBreak,
            onCheckedChange = onSwitchChanged,
            colors = SwitchDefaults.colors(
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
        state = TimerUiState(mode = TimerMode.POMODORO, time = "25:00", minutesToday = "0 min"),
        title = "Pomodoro",
        onAction = {},
        onSwitchChanged = {}
    )
}
