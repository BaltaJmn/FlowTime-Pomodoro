package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.CircularButton
import com.baltajmn.flowtime.features.screens.common.TimerState

@Composable
fun <T : TimerState<T>> ButtonsContentLandscape(
    state: T,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {
            CircularButton(
                onClick = { onFinishClick.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            if (state.isBreakRunning.not()) {
                Spacer(modifier = Modifier.height(64.dp))

                CircularButton(
                    onClick = { onBreakClick.invoke() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        } else {
            CircularButton(
                onClick = { onStartClick.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}