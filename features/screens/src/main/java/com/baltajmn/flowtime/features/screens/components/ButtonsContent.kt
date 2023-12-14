package com.baltajmn.flowtime.features.screens.components

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
import com.baltajmn.flowtime.features.screens.flowtime.FlowTimeState

@Composable
fun ButtonsContent(
    state: FlowTimeState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {

            CircularButton(
                onClick = { onFinishClick.invoke() },
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            if (state.isBreakRunning.not()) {
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
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}