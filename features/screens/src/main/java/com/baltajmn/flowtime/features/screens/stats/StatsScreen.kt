package com.baltajmn.flowtime.features.screens.stats

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.annotation.StringRes
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.TagPalette
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.stats.Level
import com.baltajmn.flowtime.data.stats.PeriodKind
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import com.baltajmn.flowtime.features.screens.history.usecases.ImportMode
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import com.baltajmn.flowtime.features.screens.pro.ProFeature
import com.baltajmn.flowtime.features.screens.pro.ProGate
import com.baltajmn.flowtime.features.screens.pro.ProLauncher
import com.baltajmn.flowtime.features.screens.settings.streakText
import java.text.NumberFormat
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinViewModel(),
    proLauncher: ProLauncher = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val csvFile = rememberLauncherForActivityResult(CreateDocument("text/csv")) { uri ->
        uri?.let(viewModel::exportCsv)
    }

    // Cada vez que se entra: puede haber sesiones nuevas.
    LaunchedEffect(Unit) { viewModel.load() }

    state.message?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message.text(context), Toast.LENGTH_LONG).show()
            viewModel.onMessageShown()
        }
    }
    state.pendingImport?.let { pending ->
        ImportConflictDialog(
            daysWithData = pending.daysWithData,
            onResolve = viewModel::resolvePendingImport
        )
    }

    StatsContent(
        state = state,
        onPeriod = viewModel::selectPeriod,
        onPrevious = viewModel::previousPeriod,
        onNext = viewModel::nextPeriod,
        onUnlock = proLauncher::open,
        onCopyHistory = { viewModel.exportStudyTime { clipboard.setText(AnnotatedString(it)) } },
        onPasteHistory = { viewModel.importStudyTime(clipboard.getText()?.text.orEmpty()) },
        onExportCsv = { csvFile.launch("flowtime-${LocalDate.now()}.csv") }
    )
}

@Composable
fun StatsContent(
    state: StatsUiState,
    onPeriod: (PeriodKind) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onUnlock: (ProFeature) -> Unit,
    onCopyHistory: () -> Unit,
    onPasteHistory: () -> Unit,
    onExportCsv: () -> Unit
) {
    val unlockStats = { onUnlock(ProFeature.STATS) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Header(
                pro = state.pro,
                onUnlockCsv = { onUnlock(ProFeature.CSV) },
                onCopyHistory = onCopyHistory,
                onPasteHistory = onPasteHistory,
                onExportCsv = onExportCsv
            )
        }
        item { LevelCard(state.level) }
        state.today?.let { today -> item { GoalProgressCard(today, state.streak) } }
        if (state.loading) return@LazyColumn
        if (!state.hasSessions) {
            item { FirstSessionCard() }
            return@LazyColumn
        }
        item { PeriodCard(state, onPeriod, onPrevious, onNext, unlockStats) }

        val details = state.details
        if (state.pro == ProAccess.HIDDEN || state.summary.totalSeconds == 0L) return@LazyColumn
        item {
            DetailCard(R.string.stats_by_hour) {
                ProGate(state.pro, unlockStats) { HourBars(details.byHour) }
            }
        }
        item {
            DetailCard(R.string.stats_by_mode) {
                val colors = MaterialTheme.colorScheme
                val palette = listOf(colors.primary, colors.tertiary, colors.secondary)
                val shares = details.byMode.map { (mode, seconds) ->
                    Share(stringResource(mode.label), palette[mode.ordinal % palette.size], seconds)
                }
                ProGate(state.pro, unlockStats) { ShareRows(shares) }
            }
        }
        // Solo con alguna etiqueta: "Sin etiqueta, 100 %" no cuenta nada.
        if (details.byTag.any { it.first != null }) {
            item {
                DetailCard(R.string.stats_by_tag) {
                    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val none = stringResource(R.string.tag_none)
                    val outline = MaterialTheme.colorScheme.outline
                    val shares = details.byTag.map { (id, seconds) ->
                        val tag = state.tags.firstOrNull { it.id == id }
                        Share(
                            name = tag?.name ?: none,
                            color = tag?.let { TagPalette.color(it.color, dark) } ?: outline,
                            seconds = seconds
                        )
                    }
                    ProGate(state.pro, unlockStats) { ShareRows(shares) }
                }
            }
        }
        if (details.topTasks.isNotEmpty()) {
            item {
                DetailCard(R.string.stats_top_tasks) {
                    ProGate(state.pro, unlockStats) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            details.topTasks.forEach { task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .semantics(mergeDescendants = true) {},
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = task.title,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = (task.seconds / 60).formatMinutesStudying(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    pro: ProAccess,
    onUnlockCsv: () -> Unit,
    onCopyHistory: () -> Unit,
    onPasteHistory: () -> Unit,
    onExportCsv: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.nav_stats),
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
            style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        )
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (pro != ProAccess.HIDDEN) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(R.string.stats_export_csv)) },
                        trailingIcon = if (pro == ProAccess.LOCKED) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lock_on),
                                    contentDescription = stringResource(R.string.pro_unlock),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            null
                        },
                        onClick = {
                            menu = false
                            if (pro == ProAccess.LOCKED) onUnlockCsv() else onExportCsv()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.stats_copy_history)) },
                    onClick = {
                        menu = false
                        onCopyHistory()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(R.string.stats_paste_history)) },
                    onClick = {
                        menu = false
                        onPasteHistory()
                    }
                )
            }
        }
    }
}

/** El nivel por los minutos de toda la vida, con el mismo cálculo que tenía Ajustes. */
@Composable
private fun LevelCard(level: Level) {
    // La barra se llena una vez al entrar, no cada vez que vuelve a pintarse.
    var animated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }
    val progress by animateFloatAsState(
        targetValue = if (animated) level.progressPercentage / 100f else 0f,
        animationSpec = tween(
            durationMillis = 1000,
            delayMillis = 100,
            easing = LinearOutSlowInEasing
        ),
        label = "level"
    )
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.user_progression_level),
                style = Title.copy(fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            )
            Text(
                text = stringResource(R.string.user_level_short, level.level),
                style = SubBody.copy(
                    fontWeight = FontWeight.W700,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
        }
    }
}

@Composable
private fun GoalProgressCard(today: DayProgress, streak: Streak) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.goal_title),
                style = Title.copy(fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            )
            Text(
                text = stringResource(
                    R.string.goal_today,
                    (today.seconds / 60).formatMinutesStudying(),
                    today.goalMinutes.toLong().formatMinutesStudying()
                ),
                style = SubBody.copy(
                    fontWeight = FontWeight.W700,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            LinearProgressIndicator(
                progress = { today.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
            )
            Text(
                text = streakText(streak),
                style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun FirstSessionCard() {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_stats),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(R.string.stats_first_session),
                style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Hoy, la semana, el mes o el año (de Pro), hacia atrás con las flechas: el resumen, la comparación
 * con el periodo anterior (de Pro) y el gráfico del periodo.
 */
@Composable
private fun PeriodCard(
    state: StatsUiState,
    onPeriod: (PeriodKind) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onUnlock: () -> Unit
) {
    val period = state.period
    val kinds = listOfNotNull(
        PeriodKind.DAY to R.string.stats_today,
        PeriodKind.WEEK to R.string.stats_week,
        PeriodKind.MONTH to R.string.stats_month,
        (PeriodKind.YEAR to R.string.stats_year).takeIf { state.pro != ProAccess.HIDDEN }
    )
    // Sin Pro, el año entero se ve difuminado; y la comparación, dentro, ya no lleva su candado.
    val yearLocked = period.kind == PeriodKind.YEAR && state.pro == ProAccess.LOCKED
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                kinds.forEachIndexed { index, (kind, label) ->
                    SegmentedButton(
                        selected = period.kind == kind,
                        onClick = { onPeriod(kind) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = kinds.size),
                        icon = {}
                    ) {
                        Text(
                            text = stringResource(label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
                    text = periodLabel(period.kind, state.range),
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
            ProGate(if (yearLocked) ProAccess.LOCKED else ProAccess.OPEN, onUnlock) {
                PeriodBody(
                    state,
                    changeAccess = if (yearLocked) ProAccess.OPEN else state.pro,
                    onUnlock
                )
            }
        }
    }
}

@Composable
private fun PeriodBody(state: StatsUiState, changeAccess: ProAccess, onUnlock: () -> Unit) {
    val summary = state.summary
    val kind = state.period.kind
    if (summary.totalSeconds == 0L) {
        Text(
            text = stringResource(R.string.stats_empty),
            modifier = Modifier.padding(vertical = 16.dp),
            style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        return
    }
    val total = (summary.totalSeconds / 60).formatMinutesStudying()
    val best = summary.bestDay?.let { day ->
        "${bestDayLabel(kind, day)} · ${(summary.bestDaySeconds / 60).formatMinutesStudying()}"
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SummaryRow(R.string.stats_total, total)
        SummaryRow(R.string.stats_sessions, summary.sessions.toString())
        SummaryRow(
            R.string.stats_average_session,
            (summary.averageSessionSeconds / 60).formatMinutesStudying()
        )
        SummaryRow(
            R.string.stats_daily_average,
            (summary.dailyAverageSeconds / 60).formatMinutesStudying()
        )
        best?.let { SummaryRow(R.string.stats_best_day, it) }
        state.details.change?.let { change ->
            ProGate(changeAccess, onUnlock) {
                SummaryRow(
                    R.string.stats_change,
                    signedPercent(change)
                )
            }
        }
    }

    // Para TalkBack, cada gráfico es un resumen en texto.
    val description = listOfNotNull(
        periodLabel(kind, state.range),
        "${stringResource(R.string.stats_total)}: $total",
        best?.let { "${stringResource(R.string.stats_best_day)}: $it" }
    ).joinToString(". ")
    val start = state.range.start
    when (kind) {
        PeriodKind.DAY -> Unit
        PeriodKind.WEEK -> PeriodBars(
            values = (0L until 7L).map { summary.byDay[start.plusDays(it)] ?: 0L },
            labels = (0L until 7L).map {
                start.plusDays(it).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
            },
            description = description
        )
        PeriodKind.MONTH -> PeriodBars(
            values = (1..start.lengthOfMonth()).map { summary.byDay[start.withDayOfMonth(it)] ?: 0L },
            labels = (1..start.lengthOfMonth()).map { if (it == 1 || it % 5 == 0) "$it" else "" },
            description = description
        )
        PeriodKind.YEAR -> {
            PeriodBars(
                values = (1..12).map { month ->
                    summary.byDay.filterKeys { YearMonth.from(it) == YearMonth.of(start.year, month) }.values.sum()
                },
                labels = (1..12).map {
                    Month.of(it).getDisplayName(TextStyle.NARROW, Locale.getDefault())
                },
                description = description
            )
            Box(modifier = Modifier.padding(top = 16.dp)) {
                YearHeatmap(year = start.year, byDay = summary.byDay, description = description)
            }
        }
    }
}

@Composable
private fun PeriodBars(values: List<Long>, labels: List<String>, description: String) {
    Box(modifier = Modifier.padding(top = 16.dp)) {
        Bars(values = values, labels = labels, description = description)
    }
}

@Composable
private fun HourBars(byHour: List<Long>) {
    val best = byHour.indices.maxByOrNull { byHour[it] } ?: 0
    Bars(
        values = byHour,
        labels = byHour.indices.map { if (it % 6 == 0) "$it" else "" },
        description = stringResource(
            R.string.stats_best_hour,
            String.format(Locale.ROOT, "%02d:00", best)
        )
    )
}

@Composable
private fun SummaryRow(@StringRes label: Int, value: String) {
    val name = stringResource(label)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "$name: $value" },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = name, style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
        Text(
            text = value,
            style = Title.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun DetailCard(@StringRes title: Int, content: @Composable () -> Unit) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(title),
                modifier = Modifier.semantics { heading() },
                style = Title.copy(fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            )
            content()
        }
    }
}

@Composable
private fun ImportConflictDialog(daysWithData: Int, onResolve: (ImportMode?) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResolve(null) },
        title = { Text(text = stringResource(R.string.import_conflict_title)) },
        text = { Text(text = stringResource(R.string.import_conflict_message, daysWithData)) },
        confirmButton = {
            Row {
                TextButton(onClick = { onResolve(ImportMode.REPLACE) }) {
                    Text(text = stringResource(R.string.import_replace))
                }
                TextButton(onClick = { onResolve(ImportMode.SUM) }) {
                    Text(text = stringResource(R.string.import_sum))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(null) }) {
                Text(text = stringResource(R.string.dialog_cancel))
            }
        }
    )
}

private fun StatsMessage.text(context: Context): String = when (this) {
    is StatsMessage.Imported -> if (days == 0) {
        context.getString(R.string.import_nothing)
    } else {
        context.getString(R.string.import_summary, days, ignoredLines)
    }
    StatsMessage.CsvSaved -> context.getString(R.string.stats_csv_saved)
    StatsMessage.CsvFailed -> context.getString(R.string.stats_csv_failed)
}

/** "+12 %", "-5 %": con el formato de porcentaje del idioma. */
private fun signedPercent(change: Float): String =
    (if (change > 0) "+" else "") + NumberFormat.getPercentInstance().format(change)

private fun periodLabel(kind: PeriodKind, range: ClosedRange<LocalDate>): String = when (kind) {
    PeriodKind.DAY -> MEDIUM.format(range.start)
    PeriodKind.WEEK -> "${MEDIUM.format(range.start)} - ${MEDIUM.format(range.endInclusive)}"
    PeriodKind.MONTH -> MONTH.format(range.start).replaceFirstChar { it.titlecase() }
    PeriodKind.YEAR -> range.start.year.toString()
}

/** En una semana basta el día; en un mes o un año hace falta la fecha. */
private fun bestDayLabel(kind: PeriodKind, day: LocalDate): String = when (kind) {
    PeriodKind.DAY, PeriodKind.WEEK -> DAY.format(day)
    PeriodKind.MONTH, PeriodKind.YEAR -> MEDIUM.format(day)
}

// Con el idioma de cada momento: se crean al pintar, no una vez al arrancar.
private val MEDIUM: DateTimeFormatter get() = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
private val MONTH: DateTimeFormatter get() = DateTimeFormatter.ofPattern("LLLL yyyy")
private val DAY: DateTimeFormatter get() = DateTimeFormatter.ofPattern("EEEE d")
