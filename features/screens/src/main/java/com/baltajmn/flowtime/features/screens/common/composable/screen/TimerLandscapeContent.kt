package com.baltajmn.flowtime.features.screens.common.composable.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.ProgressRing
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.features.screens.common.composable.components.ButtonsContent
import com.baltajmn.flowtime.features.screens.common.composable.components.MinutesStudying
import com.baltajmn.flowtime.features.screens.common.composable.components.TagChips
import com.baltajmn.flowtime.features.screens.common.composable.components.TaskChip
import com.baltajmn.flowtime.features.screens.common.composable.components.TaskDoneQuestion
import com.baltajmn.flowtime.features.screens.common.composable.components.TimeContent
import com.baltajmn.flowtime.features.screens.common.composable.components.TimerHintText
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.timer.TimerUiState

@Composable
fun TimerLandscapeContent(
    state: TimerUiState,
    title: String,
    onAction: (TimerAction) -> Unit,
    onSwitchChanged: (Boolean) -> Unit,
    onTagSelected: (Long?) -> Unit = {},
    onTaskSelected: (Task?) -> Unit = {},
    onTaskDone: () -> Unit = {},
    onTaskNotYet: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = LargeTitle.copy(
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                val color = if (state.isBreak) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                }
                ProgressRing(
                    progress = state.progress,
                    modifier = Modifier.size(200.dp),
                    color = color
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        TimeContent(
                            secondsFormatted = state.time,
                            fontSize = if (state.time.length > 5) 32.sp else 44.sp,
                            color = color
                        )
                        TimerHintText(hint = state.hint)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                MinutesStudying(minutesStudying = state.minutesToday, goal = state.goalToday)
                Spacer(modifier = Modifier.height(8.dp))
                TagChips(tags = state.tags, selected = state.tagId, onSelect = onTagSelected)
                TaskChip(
                    title = state.taskTitle,
                    pending = state.pendingTasks,
                    onSelect = onTaskSelected
                )
                if (state.askTaskDone && state.taskTitle != null) {
                    TaskDoneQuestion(
                        title = state.taskTitle,
                        onYes = onTaskDone,
                        onNotYet = onTaskNotYet
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ButtonsContent(state = state, onAction = onAction, vertical = true)

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.pomodoro_continue_after_break),
                    style = Title.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )

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
    }
}
