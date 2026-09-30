package com.baltajmn.flowtime.features.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.ceil

/**
 * Una barra por valor; la más alta llena el alto. Debajo, [labels] (las vacías no se pintan). Para
 * TalkBack, el gráfico entero es [description].
 */
@Composable
fun Bars(values: List<Long>, labels: List<String>, description: String, height: Dp = 140.dp) {
    val color = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val measurer = rememberTextMeasurer()
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .semantics { contentDescription = description }
    ) {
        val labelHeight = 18.dp.toPx()
        val chartHeight = size.height - labelHeight
        val slot = size.width / values.size
        val barWidth = slot * 0.7f
        val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1L
        values.forEachIndexed { index, value ->
            // Un día sin nada se ve como una raya, para que se sepa que está ahí.
            val barHeight = maxOf(chartHeight * value / max, 2.dp.toPx())
            drawRoundRect(
                color = if (value > 0) color else empty,
                topLeft = Offset(index * slot + (slot - barWidth) / 2, chartHeight - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(minOf(barWidth / 2, 4.dp.toPx()))
            )
            val label = labels.getOrNull(index).orEmpty()
            if (label.isNotEmpty()) {
                val text = measurer.measure(label, labelStyle)
                val x = (index * slot + slot / 2 - text.size.width / 2).coerceIn(
                    0f,
                    size.width - text.size.width
                )
                drawText(text, topLeft = Offset(x, chartHeight + 4.dp.toPx()))
            }
        }
    }
}

/**
 * El año en cuadrados, una columna por semana de lunes a domingo, con 5 tonos del color principal:
 * nada, y cuartos del día con más tiempo.
 */
@Composable
fun YearHeatmap(year: Int, byDay: Map<LocalDate, Long>, description: String) {
    val color = MaterialTheme.colorScheme.primary
    val empty = MaterialTheme.colorScheme.outlineVariant
    val first = LocalDate.of(year, 1, 1)
    val start = first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    // 53 semanas, o 54 si un año bisiesto empieza en domingo.
    val weeks = ChronoUnit.DAYS.between(start, first.plusYears(1).minusDays(1)).toInt() / 7 + 1
    val max = byDay.values.maxOrNull() ?: 0L
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(weeks / 7f)
            .semantics { contentDescription = description }
    ) {
        val cell = size.width / weeks
        val gap = cell * 0.18f
        var day = first
        while (day.year == year) {
            val index = ChronoUnit.DAYS.between(start, day).toInt()
            val seconds = byDay[day] ?: 0L
            drawRoundRect(
                color = if (seconds > 0 && max > 0) {
                    color.copy(
                        alpha = ceil(4f * seconds / max) / 4f
                    )
                } else {
                    empty
                },
                topLeft = Offset(index / 7 * cell, index % 7 * cell),
                size = Size(cell - gap, cell - gap),
                cornerRadius = CornerRadius(gap)
            )
            day = day.plusDays(1)
        }
    }
}

/** Una parte del tiempo: un modo o una etiqueta. */
data class Share(val name: String, val color: Color, val seconds: Long)

/** Cada parte con su porcentaje y su tiempo, y una barra horizontal. */
@Composable
fun ShareRows(shares: List<Share>) {
    val total = shares.sumOf { it.seconds }.coerceAtLeast(1)
    val percent = NumberFormat.getPercentInstance()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        shares.forEach { share ->
            val fraction = share.seconds.toFloat() / total
            Column(
                modifier = Modifier.semantics(mergeDescendants = true) {},
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = share.name, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${percent.format(fraction)} · ${(share.seconds / 60).formatMinutesStudying()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(share.color)
                    )
                }
            }
        }
    }
}
