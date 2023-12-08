package com.baltajmn.flowtime.features.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.CircularButton
import com.baltajmn.flowtime.core.design.theme.SubBody
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
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            if (state.isBreakRunning.not()) {
                CircularButton(
                    onClick = { onBreakClick.invoke() },
                    modifier = Modifier.size(80.dp)
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.button_break),
                        style = SubBody.copy(
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
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