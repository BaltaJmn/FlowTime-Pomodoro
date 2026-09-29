package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.annotation.DrawableRes
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
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.timer.TimerUiState

@DrawableRes
private fun TimerAction.icon() = when (this) {
    TimerAction.START, TimerAction.RESUME -> R.drawable.ic_play
    TimerAction.STOP -> R.drawable.ic_stop
    TimerAction.PAUSE -> R.drawable.ic_pause
    TimerAction.BREAK, TimerAction.SKIP_BREAK -> R.drawable.ic_next
}

@Composable
fun ButtonsContent(
    state: TimerUiState,
    onAction: (TimerAction) -> Unit,
    vertical: Boolean = false
) {
    val actions = state.actions
    val content = @Composable {
        actions.forEach { action ->
            CircularButton(onClick = { onAction(action) }) {
                Icon(
                    painter = painterResource(id = action.icon()),
                    contentDescription = stringResource(action.label),
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
            horizontalArrangement = if (actions.size > 1) Arrangement.SpaceEvenly else Arrangement.Center,
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
