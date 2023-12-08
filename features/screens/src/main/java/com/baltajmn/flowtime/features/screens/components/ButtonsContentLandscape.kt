package com.baltajmn.flowtime.features.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.CircularButton
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.features.screens.flowtime.FlowTimeState

@Composable
fun ButtonsContentLandscape(
    state: FlowTimeState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {

            CircularButton(
                onClick = { onFinishClick.invoke() },
                modifier = Modifier.size(80.dp)
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
                    onClick = { onBreakClick.invoke() },
                    modifier = Modifier.size(80.dp)
                ) {
                    Text(
                        text = "Break",
                        style = SubBody.copy(color = MaterialTheme.colorScheme.tertiary)
                    )
                }
            }

        } else {
            CircularButton(
                onClick = { onStartClick.invoke() },
                modifier = Modifier.size(80.dp)
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