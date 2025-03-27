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
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.CircularButton
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
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {
            CircularButton(onClick = onFinishClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Detener temporizador",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            if (!state.isBreakRunning && onBreakClick != null) {
                CircularButton(onClick = onBreakClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Iniciar descanso",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        } else {
            CircularButton(onClick = onStartClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Iniciar temporizador",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
