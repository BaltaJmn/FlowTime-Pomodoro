package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.CircularButton
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.timer.TimerAction
import com.baltajmn.flowtime.features.screens.timer.TimerUiState

private class TimerButton(
    val action: TimerAction,
    @DrawableRes val icon: Int,
    @StringRes val description: Int
)

private val Start = TimerButton(TimerAction.START, R.drawable.ic_play, R.string.cd_start_timer)
private val Stop = TimerButton(TimerAction.STOP, R.drawable.ic_stop, R.string.cd_stop_timer)
private val Pause = TimerButton(TimerAction.PAUSE, R.drawable.ic_pause, R.string.cd_pause_timer)
private val Resume = TimerButton(TimerAction.RESUME, R.drawable.ic_play, R.string.cd_resume_timer)
private val Break = TimerButton(TimerAction.BREAK, R.drawable.ic_next, R.string.cd_start_break)
private val SkipBreak = TimerButton(
    TimerAction.SKIP_BREAK,
    R.drawable.ic_next,
    R.string.cd_skip_break
)

private fun buttonsFor(state: TimerUiState): List<TimerButton> = when {
    !state.isActive -> listOf(Start)
    state.paused -> listOf(Stop, Resume)
    state.phase == Phase.BREAK -> listOf(Stop, Pause, SkipBreak)
    state.canTakeBreak -> listOf(Stop, Pause, Break)
    else -> listOf(Stop, Pause)
}

@Composable
fun ButtonsContent(
    state: TimerUiState,
    onAction: (TimerAction) -> Unit,
    vertical: Boolean = false
) {
    val buttons = buttonsFor(state)
    val content = @Composable {
        buttons.forEach { button ->
            CircularButton(onClick = { onAction(button.action) }) {
                Icon(
                    painter = painterResource(id = button.icon),
                    contentDescription = stringResource(button.description),
                    tint = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
    if (vertical) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically)
        ) { content() }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (buttons.size > 1) Arrangement.SpaceEvenly else Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
}

@Preview(showBackground = true)
@Composable
fun ButtonsContentRunningPreview() {
    ButtonsContent(
        state = TimerUiState(mode = TimerMode.FLOW_TIME, phase = Phase.WORK),
        onAction = {}
    )
}
