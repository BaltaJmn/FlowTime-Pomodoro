package com.baltajmn.flowtime.features.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.data.stats.PeriodKind
import com.baltajmn.flowtime.data.stats.StatsPeriod
import com.baltajmn.flowtime.data.stats.StatsSummary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * El resumen de las estadísticas (#38), lo gratis: hoy, la semana o el mes, hacia atrás con las
 * flechas. Lo de Pro (el año, por hora, por etiqueta, la comparación y el CSV) espera a que exista.
 */
@Composable
fun StatsSummaryCard(
    period: StatsPeriod,
    range: ClosedRange<LocalDate>,
    summary: StatsSummary,
    onPeriod: (PeriodKind) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    PeriodKind.DAY to R.string.stats_today,
                    PeriodKind.WEEK to R.string.stats_week,
                    PeriodKind.MONTH to R.string.stats_month
                ).forEach { (kind, label) ->
                    FilterChip(
                        selected = period.kind == kind,
                        onClick = { onPeriod(kind) },
                        label = { Text(text = stringResource(label)) }
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.stats_previous),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = label(period.kind, range),
                    modifier = Modifier.weight(1f),
                    style = Title.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.primary),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNext, enabled = period.offset > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.stats_next),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (summary.totalSeconds == 0L) {
                Text(
                    text = stringResource(R.string.stats_empty),
                    modifier = Modifier.padding(vertical = 16.dp),
                    style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                return@Column
            }
            val rows = listOf(
                R.string.stats_total to (summary.totalSeconds / 60).formatMinutesStudying(),
                R.string.stats_sessions to summary.sessions.toString(),
                R.string.stats_average_session to (summary.averageSessionSeconds / 60).formatMinutesStudying(),
                R.string.stats_daily_average to (summary.dailyAverageSeconds / 60).formatMinutesStudying()
            ) + listOfNotNull(
                summary.bestDay?.let { day ->
                    R.string.stats_best_day to "${DAY.format(day)} · ${(summary.bestDaySeconds / 60).formatMinutesStudying()}"
                }
            )
            val colors = MaterialTheme.colorScheme
            rows.forEach { (label, value) ->
                val name = stringResource(label)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .semantics(mergeDescendants = true) { contentDescription = "$name: $value" },
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = name, style = SubBody.copy(color = colors.onSurfaceVariant))
                    Text(text = value, style = Title.copy(fontSize = 16.sp, color = colors.primary))
                }
            }
        }
    }
}

private fun label(kind: PeriodKind, range: ClosedRange<LocalDate>): String = when (kind) {
    PeriodKind.DAY -> MEDIUM.format(range.start)
    PeriodKind.WEEK -> "${MEDIUM.format(range.start)} – ${MEDIUM.format(range.endInclusive)}"
    PeriodKind.MONTH -> MONTH.format(range.start).replaceFirstChar { it.titlecase() }
    PeriodKind.YEAR -> range.start.year.toString()
}

// Con el idioma de cada momento: se crean al pintar, no una vez al arrancar.
private val MEDIUM: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val MONTH: DateTimeFormatter get() = DateTimeFormatter.ofPattern("LLLL yyyy")
private val DAY: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEEE d")
