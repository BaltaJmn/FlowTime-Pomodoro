package com.baltajmn.flowtime.features.screens.stats

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import com.baltajmn.flowtime.data.tag.Tag
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.KeyboardType
import com.baltajmn.flowtime.data.repository.FocusSession
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import com.baltajmn.flowtime.core.design.components.FlowCard
import com.baltajmn.flowtime.core.design.components.quietSegmentedColors
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.features.screens.common.composable.components.ScreenTitle
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
import com.baltajmn.flowtime.core.design.extensions.readableWidth
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import kotlinx.datetime.plus
import kotlinx.datetime.number
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import com.baltajmn.flowtime.data.goal.today
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.getString
import com.baltajmn.flowtime.features.screens.platform.rememberShowMessage
import com.baltajmn.flowtime.features.screens.platform.rememberCreateFile
import com.baltajmn.flowtime.features.screens.platform.Formats
import kotlinx.datetime.Month

@Composable
fun StatsScreen(
    viewModel: StatsViewModel = koinViewModel(),
    proLauncher: ProLauncher = koinInject(),
    /** Venir a ver hoy: el periodo pasa al día. */
    openToday: Boolean = false,
    onTodayOpened: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val showMessage = rememberShowMessage()
    val clipboard = LocalClipboardManager.current
    val csvFile = rememberCreateFile("text/csv", viewModel::exportCsv)

    // Cada vez que se entra: puede haber sesiones nuevas.
    LaunchedEffect(openToday) {
        if (openToday) {
            viewModel.selectPeriod(PeriodKind.DAY)
            onTodayOpened()
        } else {
            viewModel.load()
        }
    }

    state.message?.let { message ->
        LaunchedEffect(message) {
            showMessage(message.text())
            viewModel.onMessageShown()
        }
    }
    state.pendingImport?.let { pending ->
        ImportConflictDialog(
            daysWithData = pending.daysWithData,
            onResolve = viewModel::resolvePendingImport
        )
    }

    // Borrar una sesión no pregunta: se puede deshacer.
    val snackbar = remember { SnackbarHostState() }
    val deletedText = stringResource(Res.string.session_deleted)
    val undoText = stringResource(Res.string.task_undo)
    LaunchedEffect(state.deletedSession) {
        if (state.deletedSession == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDeleteSession() else viewModel.onSessionDeletedShown()
    }

    Box(modifier = Modifier.fillMaxSize()) {
    StatsContent(
        state = state,
        onPeriod = viewModel::selectPeriod,
        onPrevious = viewModel::previousPeriod,
        onNext = viewModel::nextPeriod,
        onUnlock = proLauncher::open,
        onDismissProCard = viewModel::dismissProCard,
        onCopyHistory = { viewModel.exportStudyTime { clipboard.setText(AnnotatedString(it)) } },
        onPasteHistory = { viewModel.importStudyTime(clipboard.getText()?.text.orEmpty()) },
        onExportCsv = { csvFile("flowtime-${today()}.csv") },
        onDeleteSession = viewModel::deleteSession,
        onSessionMinutes = viewModel::setSessionMinutes,
        onAddMinutes = viewModel::addMinutes
    )
    SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun StatsContent(
    state: StatsUiState,
    onPeriod: (PeriodKind) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onUnlock: (ProFeature) -> Unit,
    onDismissProCard: () -> Unit,
    onCopyHistory: () -> Unit,
    onPasteHistory: () -> Unit,
    onExportCsv: () -> Unit,
    onDeleteSession: (Long) -> Unit = {},
    onSessionMinutes: (Long, Long) -> Unit = { _, _ -> },
    onAddMinutes: (Long) -> Unit = {}
) {
    val unlockStats = { onUnlock(ProFeature.STATS) }
    val summary: LazyListScope.() -> Unit = summary@{
        item {
            Header(
                pro = state.pro,
                onUnlockCsv = { onUnlock(ProFeature.CSV) },
                onCopyHistory = onCopyHistory,
                onPasteHistory = onPasteHistory,
                onExportCsv = onExportCsv
            )
        }
        state.today?.let { today -> item { GoalProgressCard(today, state.streak) } }
        if (state.loading) return@summary
        if (!state.hasSessions) {
            item { FirstSessionCard(onAddMinutes) }
            return@summary
        }
        item {
            PeriodCard(state, onPeriod, onPrevious, onNext, unlockStats) {
                DaySessions(state.daySessions, state.tags, onDeleteSession, onSessionMinutes, onAddMinutes)
            }
        }
        if (state.showProCard) {
            item {
                ProStreakCard(
                    state.streak.current,
                    onOpen = unlockStats,
                    onDismiss = onDismissProCard
                )
            }
        }
        // El nivel, después de los números del periodo: es lo de siempre, no lo de hoy.
        item { LevelCard(state.level) }
    }
    val details = state.details
    val hasDetails = !state.loading && state.hasSessions && state.pro != ProAccess.HIDDEN &&
        state.summary.totalSeconds > 0L
    val detailCards: LazyListScope.() -> Unit = {
        item {
            DetailCard(Res.string.stats_by_hour) {
                ProGate(state.pro, unlockStats) { HourBars(details.byHour) }
            }
        }
        item {
            DetailCard(Res.string.stats_by_mode) {
                val colors = MaterialTheme.colorScheme
                // Sin tertiary: en la pantalla de enfoque es el color del descanso.
                val palette = listOf(colors.primary, colors.secondary, colors.outline)
                val shares = details.byMode.map { (mode, seconds) ->
                    Share(stringResource(mode.label), palette[mode.ordinal % palette.size], seconds)
                }
                ProGate(state.pro, unlockStats) { ShareRows(shares) }
            }
        }
        // Solo con alguna etiqueta: "Sin etiqueta, 100 %" no cuenta nada.
        if (details.byTag.any { it.first != null }) {
            item {
                DetailCard(Res.string.stats_by_tag) {
                    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
                    val none = stringResource(Res.string.tag_none)
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
                DetailCard(Res.string.stats_top_tasks) {
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

    // Desde 840 dp (#46), el resumen a un lado y los gráficos al otro; si no, una columna centrada.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (hasDetails && maxWidth >= 840.dp) {
            Row {
                StatsList(Modifier.weight(1f), summary)
                StatsList(Modifier.weight(1f), detailCards)
            }
        } else {
            StatsList(Modifier.readableWidth()) {
                summary()
                if (hasDetails) detailCards()
            }
        }
    }
}

@Composable
private fun StatsList(modifier: Modifier, content: LazyListScope.() -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .then(modifier)
            .windowInsetsPadding(WindowInsets.statusBars),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
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
        ScreenTitle(text = stringResource(Res.string.nav_stats), modifier = Modifier.weight(1f))
        Box {
            IconButton(onClick = { menu = true }) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = stringResource(Res.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                if (pro != ProAccess.HIDDEN) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(Res.string.stats_export_csv)) },
                        trailingIcon = if (pro == ProAccess.LOCKED) {
                            {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_lock_on),
                                    contentDescription = stringResource(Res.string.pro_unlock),
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
                    text = { Text(text = stringResource(Res.string.stats_copy_history)) },
                    onClick = {
                        menu = false
                        onCopyHistory()
                    }
                )
                DropdownMenuItem(
                    text = { Text(text = stringResource(Res.string.stats_paste_history)) },
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
    FlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.user_progression_level),
                style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = stringResource(Res.string.user_level_short, level.level),
                style = SubBody.copy(
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                drawStopIndicator = {}
            )
            Text(
                text = stringResource(Res.string.level_hint),
                style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun GoalProgressCard(today: DayProgress, streak: Streak) {
    FlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.goal_title),
                style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Text(
                text = stringResource(
                    Res.string.goal_today,
                    (today.seconds / 60).formatMinutesStudying(),
                    today.goalMinutes.toLong().formatMinutesStudying()
                ),
                style = SubBody.copy(
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            LinearProgressIndicator(
                progress = { today.fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                drawStopIndicator = {}
            )
            Text(
                text = streakText(streak),
                style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

/** Sin sesiones todavía. Se puede añadir el tiempo de antes de usar la app, o el que no se cronometró. */
@Composable
private fun FirstSessionCard(onAdd: (Long) -> Unit) {
    var adding by rememberSaveable { mutableStateOf(false) }
    if (adding) {
        MinutesDialog(
            title = Res.string.sessions_add,
            initial = null,
            onDismiss = { adding = false },
            onSave = { minutes ->
                adding = false
                onAdd(minutes)
            }
        )
    }
    FlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_stats),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = stringResource(Res.string.stats_first_session),
                style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                textAlign = TextAlign.Center
            )
            TextButton(onClick = { adding = true }) { Text(text = stringResource(Res.string.sessions_add)) }
        }
    }
}

/** Sin Pro y con una racha de 7 días: se puede cerrar, y entonces no vuelve en 30 días. */
@Composable
private fun ProStreakCard(days: Int, onOpen: () -> Unit, onDismiss: () -> Unit) {
    FlowCard {
        Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
            Row(
                modifier = Modifier.padding(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_fire),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pluralStringResource(Res.plurals.streak_days, days, days),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(Res.string.pro_card_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(Res.string.pro_card_dismiss)
                    )
                }
                TextButton(onClick = onOpen) { Text(text = stringResource(Res.string.pro_see)) }
            }
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
    onUnlock: () -> Unit,
    dayContent: @Composable () -> Unit = {}
) {
    val period = state.period
    val kinds = listOfNotNull(
        PeriodKind.DAY to Res.string.stats_today,
        PeriodKind.WEEK to Res.string.stats_week,
        PeriodKind.MONTH to Res.string.stats_month,
        (PeriodKind.YEAR to Res.string.stats_year).takeIf { state.pro != ProAccess.HIDDEN }
    )
    // Sin Pro, el año entero se ve difuminado; y la comparación, dentro, ya no lleva su candado.
    val yearLocked = period.kind == PeriodKind.YEAR && state.pro == ProAccess.LOCKED
    FlowCard {
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
                        colors = quietSegmentedColors(),
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
                        contentDescription = stringResource(Res.string.stats_previous),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = periodLabel(period.kind, state.range),
                    modifier = Modifier.weight(1f),
                    style = Title.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onNext, enabled = period.offset > 0) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(Res.string.stats_next),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
            if (period.kind == PeriodKind.DAY) dayContent()
        }
    }
}

/**
 * Las sesiones de un día, para corregir lo que el reloj no supo: la que se quedó contando, la que
 * no se cronometró. Tocar una la edita.
 */
@Composable
private fun DaySessions(
    sessions: List<FocusSession>,
    tags: List<Tag>,
    onDelete: (Long) -> Unit,
    onMinutes: (Long, Long) -> Unit,
    onAdd: (Long) -> Unit
) {
    var editing by remember { mutableStateOf<FocusSession?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        sessions.forEach { session ->
            val minutes = (session.focusSeconds / 60).formatMinutesStudying()
            val name = if (session.added) {
                stringResource(Res.string.session_added)
            } else {
                "${clockTime(session.startedAt)} - ${clockTime(session.endedAt)}"
            }
            // En qué fue, para reconocerla sin recordar la hora.
            val what = listOfNotNull(tags.firstOrNull { it.id == session.tagId }?.name, session.taskTitle)
                .joinToString(", ")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClickLabel = stringResource(Res.string.session_edit)) { editing = session }
                    .semantics(mergeDescendants = true) {}
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = SubBody.copy(color = MaterialTheme.colorScheme.onSurface))
                    if (what.isNotEmpty()) {
                        Text(
                            text = what,
                            style = SubBody.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(text = minutes, style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant))
                // Que se puede tocar para corregirla.
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        TextButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.Start)) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(Res.string.sessions_add))
        }
    }

    editing?.let { session ->
        MinutesDialog(
            title = Res.string.session_edit,
            initial = session.focusSeconds / 60,
            onDismiss = { editing = null },
            onSave = { minutes ->
                editing = null
                onMinutes(session.id, minutes)
            },
            onDelete = {
                editing = null
                onDelete(session.id)
            }
        )
    }
    if (adding) {
        MinutesDialog(
            title = Res.string.sessions_add,
            initial = null,
            onDismiss = { adding = false },
            onSave = { minutes ->
                adding = false
                onAdd(minutes)
            }
        )
    }
}

/** Los minutos de una sesión, de 1 a un día entero. Sin un número válido, Guardar no se puede pulsar. */
@Composable
private fun MinutesDialog(
    title: StringResource,
    initial: Long?,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var text by rememberSaveable { mutableStateOf(initial?.toString().orEmpty()) }
    val minutes = text.toLongOrNull()?.takeIf { it in 1..MINUTES_PER_DAY }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(title)) },
        text = {
            Column {
                // Fuera de rango, se dice por qué Guardar no se puede pulsar.
                TextField(
                    value = text,
                    onValueChange = { value -> text = value.filter(Char::isDigit).take(4) },
                    label = { Text(text = stringResource(Res.string.session_minutes)) },
                    supportingText = { Text(text = stringResource(Res.string.session_minutes_range, MINUTES_PER_DAY)) },
                    isError = text.isNotEmpty() && minutes == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                // Borrar, lejos de Cancelar y Guardar: se puede deshacer, pero no se pulsa por error.
                onDelete?.let { delete ->
                    TextButton(onClick = delete, modifier = Modifier.padding(top = 8.dp)) {
                        Text(text = stringResource(Res.string.mix_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { minutes?.let(onSave) }, enabled = minutes != null) {
                Text(text = stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.dialog_cancel)) }
        }
    )
}

private const val MINUTES_PER_DAY = 24 * 60L

/** La hora de un instante en la del móvil, en el formato del idioma. */
private fun clockTime(millis: Long): String =
    Formats.shortTime(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).time)

@Composable
private fun PeriodBody(state: StatsUiState, changeAccess: ProAccess, onUnlock: () -> Unit) {
    val summary = state.summary
    val kind = state.period.kind
    if (summary.totalSeconds == 0L) {
        Text(
            text = stringResource(Res.string.stats_empty),
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
        SummaryRow(Res.string.stats_total, total)
        SummaryRow(Res.string.stats_sessions, summary.sessions.toString())
        SummaryRow(
            Res.string.stats_average_session,
            (summary.averageSessionSeconds / 60).formatMinutesStudying()
        )
        // En un solo día, la media diaria y el mejor día repetirían el total.
        if (kind != PeriodKind.DAY) {
            SummaryRow(
                Res.string.stats_daily_average,
                (summary.dailyAverageSeconds / 60).formatMinutesStudying()
            )
            best?.let { SummaryRow(Res.string.stats_best_day, it) }
        }
        state.details.change?.let { change ->
            ProGate(changeAccess, onUnlock) {
                SummaryRow(
                    Res.string.stats_change,
                    signedPercent(change)
                )
            }
        }
    }

    // Para TalkBack, cada gráfico es un resumen en texto.
    val description = listOfNotNull(
        periodLabel(kind, state.range),
        "${stringResource(Res.string.stats_total)}: $total",
        best?.let { "${stringResource(Res.string.stats_best_day)}: $it" }
    ).joinToString(". ")
    val start = state.range.start
    when (kind) {
        PeriodKind.DAY -> Unit
        PeriodKind.WEEK -> PeriodBars(
            values = (0 until 7).map { summary.byDay[start.plus(it, DateTimeUnit.DAY)] ?: 0L },
            labels = (0 until 7).map {
                Formats.narrowWeekday(start.plus(it, DateTimeUnit.DAY).dayOfWeek)
            },
            description = description
        )
        PeriodKind.MONTH -> PeriodBars(
            values = (1..state.range.endInclusive.day).map { summary.byDay[LocalDate(start.year, start.month, it)] ?: 0L },
            labels = (1..state.range.endInclusive.day).map { if (it == 1 || it % 5 == 0) "$it" else "" },
            description = description
        )
        PeriodKind.YEAR -> {
            PeriodBars(
                values = (1..12).map { month ->
                    summary.byDay.filterKeys { it.year == start.year && it.month.number == month }.values.sum()
                },
                labels = Month.entries.map(Formats::narrowMonth),
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
            Res.string.stats_best_hour,
            "${best.toString().padStart(2, '0')}:00"
        )
    )
}

@Composable
private fun SummaryRow(label: StringResource, value: String) {
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
private fun DetailCard(title: StringResource, content: @Composable () -> Unit) {
    FlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(title),
                modifier = Modifier.semantics { heading() },
                style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            content()
        }
    }
}

@Composable
private fun ImportConflictDialog(daysWithData: Int, onResolve: (ImportMode?) -> Unit) {
    AlertDialog(
        onDismissRequest = { onResolve(null) },
        title = { Text(text = stringResource(Res.string.import_conflict_title)) },
        text = { Text(text = stringResource(Res.string.import_conflict_message, daysWithData)) },
        confirmButton = {
            Row {
                TextButton(onClick = { onResolve(ImportMode.REPLACE) }) {
                    Text(text = stringResource(Res.string.import_replace))
                }
                TextButton(onClick = { onResolve(ImportMode.SUM) }) {
                    Text(text = stringResource(Res.string.import_sum))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onResolve(null) }) {
                Text(text = stringResource(Res.string.dialog_cancel))
            }
        }
    )
}

private suspend fun StatsMessage.text(): String = when (this) {
    is StatsMessage.Imported -> if (days == 0) {
        getString(Res.string.import_nothing)
    } else {
        getString(Res.string.import_summary, days, ignoredLines)
    }
    StatsMessage.CsvSaved -> getString(Res.string.stats_csv_saved)
    StatsMessage.CsvFailed -> getString(Res.string.stats_csv_failed)
}

/** "+12 %", "-5 %": con el formato de porcentaje del idioma. */
private fun signedPercent(change: Float): String =
    (if (change > 0) "+" else "") + Formats.percent(change)

private fun periodLabel(kind: PeriodKind, range: ClosedRange<LocalDate>): String = when (kind) {
    PeriodKind.DAY -> Formats.mediumDate(range.start)
    PeriodKind.WEEK -> "${Formats.mediumDate(range.start)} - ${Formats.mediumDate(range.endInclusive)}"
    PeriodKind.MONTH -> Formats.monthYear(range.start).replaceFirstChar { it.titlecase() }
    PeriodKind.YEAR -> range.start.year.toString()
}

/** En una semana basta el día; en un mes o un año hace falta la fecha. */
private fun bestDayLabel(kind: PeriodKind, day: LocalDate): String = when (kind) {
    PeriodKind.DAY, PeriodKind.WEEK -> Formats.weekdayDay(day)
    PeriodKind.MONTH, PeriodKind.YEAR -> Formats.mediumDate(day)
}
