package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.LargeTitle

@Composable
fun TimeContent(
    secondsFormatted: String
) {
    if (secondsFormatted.split(":").size == 2) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = secondsFormatted.split(":")[0],
                    style = LargeTitle.copy(
                        fontSize = 100.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.End
                )
            }

            Column(
                modifier = Modifier.weight(0.1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = ":",
                    style = LargeTitle.copy(
                        fontSize = 100.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.weight(0.5f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = secondsFormatted.split(":")[1],
                    style = LargeTitle.copy(
                        fontSize = 100.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.Start
                )
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .padding(end = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = secondsFormatted.split(":")[0],
                    style = LargeTitle.copy(
                        fontSize = 70.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.End
                )
            }

            Column(
                modifier = Modifier.weight(0.05f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = ":",
                    style = LargeTitle.copy(
                        fontSize = 70.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.weight(0.25f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = secondsFormatted.split(":")[1],
                    style = LargeTitle.copy(
                        fontSize = 70.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.weight(0.05f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = ":",
                    style = LargeTitle.copy(
                        fontSize = 70.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .weight(0.3f)
                    .padding(start = 4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = secondsFormatted.split(":")[2],
                    style = LargeTitle.copy(
                        fontSize = 70.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}