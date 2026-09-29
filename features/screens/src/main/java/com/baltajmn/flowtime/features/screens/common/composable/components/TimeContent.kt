package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.LargeTitle

@Composable
fun TimeContent(
    secondsFormatted: String
) {
    val textStyle = LargeTitle.copy(
        fontSize = 100.sp,
        color = MaterialTheme.colorScheme.primary
    )

    val description = timeDescription(secondsFormatted)

    // Horas, minutos, ":" y segundos son textos sueltos: TalkBack los leía por separado ("25",
    // "dos puntos", "00"). Sin región viva, que hablaría cada segundo.
    Box(modifier = Modifier.clearAndSetSemantics { contentDescription = description }) {
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
                        style = textStyle,
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
                        style = textStyle,
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
                        style = textStyle,
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
                        style = textStyle,
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
}

@Composable
private fun timeDescription(secondsFormatted: String): String {
    val parts = secondsFormatted.split(":").map { it.trim().toIntOrNull() ?: 0 }
    val hours = if (parts.size == 3) parts.first() else 0
    val minutes = parts.getOrElse(parts.size - 2) { 0 }
    val seconds = parts.lastOrNull() ?: 0

    val hoursText = pluralStringResource(R.plurals.time_hours, hours, hours)
    val minutesText = pluralStringResource(R.plurals.time_minutes, minutes, minutes)
    val secondsText = pluralStringResource(R.plurals.time_seconds, seconds, seconds)
    return if (hours > 0) "$hoursText $minutesText $secondsText" else "$minutesText $secondsText"
}

@Preview
@Composable
fun TimeContentPreview() {
    TimeContent("00:00")
}