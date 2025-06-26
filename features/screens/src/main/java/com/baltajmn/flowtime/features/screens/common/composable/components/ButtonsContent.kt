package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.CircularButton
import com.baltajmn.flowtime.features.screens.common.FlowTimeState
import com.baltajmn.flowtime.features.screens.common.TimerState

@Composable
fun <T : TimerState<T>> ButtonsContent(
    state: T,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
    onBreakClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (state.isTimerRunning || state.isBreakRunning) {
            Arrangement.SpaceEvenly
        } else {
            Arrangement.Center
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {
            CircularButton(onClick = onFinishClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Detener temporizador",
                    tint = MaterialTheme.colorScheme.surface
                )
            }
            if (!state.isBreakRunning && onBreakClick != null) {
                CircularButton(onClick = onBreakClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Iniciar descanso",
                        tint = MaterialTheme.colorScheme.surface
                    )
                }
            }
        } else {
            CircularButton(onClick = onStartClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Iniciar temporizador",
                    tint = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ButtonsContentRunningPreview() {
    ButtonsContent(
        state = FlowTimeState().copyTimer(isTimerRunning = true),
        onStartClick = {},
        onFinishClick = {},
        onBreakClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun ButtonsContentStoppedPreview() {
    ButtonsContent(
        state = FlowTimeState(),
        onStartClick = {},
        onFinishClick = {}
    )
}
