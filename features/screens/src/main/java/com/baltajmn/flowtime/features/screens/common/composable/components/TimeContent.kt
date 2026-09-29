package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.LargeTitle

/**
 * El tiempo, con cada cifra en una caja del ancho de la más ancha: Poppins no tiene cifras
 * tabulares, y sin esto el texto se movía cada segundo.
 */
@Composable
fun TimeContent(
    secondsFormatted: String,
    fontSize: TextUnit = 100.sp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val style = LargeTitle.copy(fontSize = fontSize, color = color)
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val digitWidth = remember(style, density) {
        with(density) { DIGITS.maxOf { measurer.measure(it.toString(), style).size.width }.toDp() }
    }
    val description = timeDescription(secondsFormatted)

    // Un solo texto para TalkBack, y sin región viva, que hablaría cada segundo.
    Row(
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        secondsFormatted.forEach { char ->
            if (char.isDigit()) {
                Box(modifier = Modifier.width(digitWidth), contentAlignment = Alignment.Center) {
                    Text(text = char.toString(), style = style)
                }
            } else {
                Text(text = char.toString(), style = style)
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

private const val DIGITS = "0123456789"

@Preview
@Composable
fun TimeContentPreview() {
    TimeContent("00:00")
}
