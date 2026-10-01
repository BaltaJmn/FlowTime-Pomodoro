package com.baltajmn.flowtime.features.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.extensions.readableWidth
import com.baltajmn.flowtime.core.design.components.ProgressRing
import com.baltajmn.flowtime.core.design.components.SoundButton
import com.baltajmn.flowtime.core.design.components.SoundSheet
import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.components.ButtonsContent
import com.baltajmn.flowtime.features.screens.common.composable.components.MinutesStudying
import com.baltajmn.flowtime.core.design.components.quietSegmentedColors
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.features.screens.common.composable.components.TagChips
import com.baltajmn.flowtime.features.screens.common.composable.components.TaskChip
import com.baltajmn.flowtime.features.screens.common.composable.components.TaskDoneQuestion
import com.baltajmn.flowtime.features.screens.common.composable.components.TimeContent
import com.baltajmn.flowtime.features.screens.common.composable.components.TimerHintText
import com.baltajmn.flowtime.features.screens.common.composable.components.advantages
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import com.baltajmn.flowtime.features.screens.edit.ModeSettingsSheet
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.data.pro.ProFeatures
import com.baltajmn.flowtime.data.pro.ProGate
import com.baltajmn.flowtime.features.screens.pro.ProFeature
import com.baltajmn.flowtime.features.screens.pro.ProLauncher
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.baltajmn.flowtime.features.screens.platform.rememberNotificationPermission
import com.baltajmn.flowtime.features.screens.platform.KeepScreenOn

private enum class FocusSheet { SOUNDS, MODE }

/** La pantalla de inicio (#51): el modo, el temporizador, empezar y el progreso de hoy. */
@Composable
fun FocusScreen(
    showSound: Boolean,
    viewModel: FocusViewModel = koinViewModel(),
    ambience: Ambience = koinInject(),
    gate: ProGate = koinInject(),
    proLauncher: ProLauncher = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sound by ambience.state.collectAsStateWithLifecycle()
    val notifications = rememberNotificationPermission()
    var explainNotifications by remember { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<FocusSheet?>(null) }

    KeepScreenOn(active = state.keepScreenOn && state.isActive && !state.paused)

    FocusContent(
        state = state,
        showSound = showSound,
        soundPlaying = sound.playing.isNotEmpty(),
        onSelectMode = viewModel::select,
        onAction = { action ->
            viewModel.onAction(action)
            // La primera sesión es cuando se entiende para qué sirve el permiso.
            if (action == TimerAction.START && !notifications.granted()) {
                explainNotifications = viewModel.explainNotificationsOnce()
            }
        },
        onOpenSounds = { sheet = FocusSheet.SOUNDS },
        onOpenModeSettings = { sheet = FocusSheet.MODE },
        onTagSelected = viewModel::selectTag,
        onTaskSelected = viewModel::selectTask,
        onTaskDone = viewModel::completeTask,
        onTaskNotYet = viewModel::keepTask
    )

    when (sheet) {
        FocusSheet.SOUNDS -> SoundSheet(
            onDismiss = { sheet = null },
            mixLimit = gate.limit(Limits.FREE_MIXES),
            onSeeProMixes = { proLauncher.open(ProFeature.MIXES) },
            showProSounds = ProFeatures.enabled,
            proSoundsLocked = gate.locked,
            onSeeProSounds = { proLauncher.open(ProFeature.SOUNDS) },
            ambience = koinInject(),
            mixes = koinInject()
        )
        FocusSheet.MODE -> ModeSettingsSheet(mode = state.mode, onDismiss = { sheet = null })
        null -> Unit
    }

    if (explainNotifications) {
        AlertDialog(
            onDismissRequest = { explainNotifications = false },
            title = { Text(stringResource(Res.string.notifications_title)) },
            text = { Text(stringResource(Res.string.notifications_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explainNotifications = false
                        notifications.ask()
                    }
                ) { Text(stringResource(Res.string.turn_on)) }
            },
            dismissButton = {
                TextButton(onClick = { explainNotifications = false }) {
                    Text(stringResource(Res.string.notifications_later))
                }
            }
        )
    }
}

@Composable
fun FocusContent(
    state: FocusUiState,
    showSound: Boolean,
    soundPlaying: Boolean,
    onSelectMode: (TimerMode) -> Unit,
    onAction: (TimerAction) -> Unit,
    onOpenSounds: () -> Unit,
    onOpenModeSettings: () -> Unit,
    onTagSelected: (Long?) -> Unit = {},
    onTaskSelected: (Task?) -> Unit = {},
    onTaskDone: () -> Unit = {},
    onTaskNotYet: () -> Unit = {}
) {
    val tools = @Composable {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            if (showSound) SoundButton(playing = soundPlaying, onClick = onOpenSounds)
            IconButton(onClick = onOpenModeSettings) {
                Icon(
                    painter = painterResource(Res.drawable.ic_tune),
                    contentDescription = stringResource(Res.string.cd_mode_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    val today = @Composable {
        MinutesStudying(minutesStudying = state.minutesToday, goal = state.goalToday)
        if (state.streak > 0) {
            Text(
                text = pluralStringResource(Res.plurals.streak_days, state.streak, state.streak),
                style = SubBody.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TagChips(tags = state.tags, selected = state.tagId, onSelect = onTagSelected)
        TaskChip(title = state.taskTitle, pending = state.pendingTasks, onSelect = onTaskSelected)
        if (state.askTaskDone && state.taskTitle != null) {
            TaskDoneQuestion(title = state.taskTitle, onYes = onTaskDone, onNotYet = onTaskNotYet)
        }
    }

    // Por el espacio que queda de verdad y no por la orientación del aparato (#46): en pantalla
    // dividida o en un plegable, la orientación no dice cuánto sitio hay.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            val ring = minOf(maxHeight * 0.6f, maxWidth / 2 - 40.dp, 320.dp)
            TwoColumns(state, ring, onAction, onSelectMode, tools, today)
        } else {
            OneColumn(
                state,
                ring = minOf(maxWidth - 32.dp, 280.dp),
                onAction,
                onSelectMode,
                tools,
                today
            )
        }
    }
}

@Composable
private fun TwoColumns(
    state: FocusUiState,
    ring: Dp,
    onAction: (TimerAction) -> Unit,
    onSelectMode: (TimerMode) -> Unit,
    tools: @Composable () -> Unit,
    today: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            PhaseTitle(state)
            Ring(state, size = ring)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(0.5f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            tools()
            // Centrado si cabe, como el anillo; si no (un móvil en horizontal), desde arriba y con scroll.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ModeSelector(state, onSelectMode)
                ModeLine(state)
                Spacer(modifier = Modifier.height(16.dp))
                ButtonsContent(state = state, onAction = onAction)
                Spacer(modifier = Modifier.height(16.dp))
                today()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun OneColumn(
    state: FocusUiState,
    ring: Dp,
    onAction: (TimerAction) -> Unit,
    onSelectMode: (TimerMode) -> Unit,
    tools: @Composable () -> Unit,
    today: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .readableWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        tools()
        ModeSelector(state, onSelectMode)
        ModeLine(state)
        Spacer(modifier = Modifier.height(24.dp))
        PhaseTitle(state)
        Ring(state, size = ring)
        Spacer(modifier = Modifier.height(32.dp))
        ButtonsContent(state = state, onAction = onAction)
        Spacer(modifier = Modifier.height(32.dp))
        today()
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Con una sesión en marcha, el selector enseña su modo y no deja cambiarlo. */
@Composable
private fun ModeSelector(state: FocusUiState, onSelect: (TimerMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TimerMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == state.mode,
                onClick = { onSelect(mode) },
                enabled = !state.isActive || mode == state.mode,
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = TimerMode.entries.size
                ),
                colors = quietSegmentedColors(),
                // Sin la marca de elegido: con ella, "Porcentaje" no cabe en un móvil estrecho.
                icon = {}
            ) {
                Text(
                    text = stringResource(mode.label),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModeLine(state: FocusUiState) {
    Text(
        text = stringResource(
            if (state.isActive) Res.string.focus_mode_locked else state.mode.advantages
        ),
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
        textAlign = TextAlign.Center,
        style = SubBody.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

/**
 * Trabajando, descansando o en pausa, como etiqueta: lo grande es el reloj. Parado no hace falta: ya
 * se ve el modo.
 */
@Composable
private fun PhaseTitle(state: FocusUiState) {
    val title = when {
        state.paused -> Res.string.time_title_paused
        state.phase == Phase.WORK -> Res.string.time_title_working
        state.phase == Phase.BREAK -> Res.string.time_title_resting
        else -> null
    }
    if (title != null) {
        Text(
            text = stringResource(title),
            style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun Ring(state: FocusUiState, size: Dp) {
    // Trabajo con primary y descanso con tertiary: se distinguen sin leer nada.
    val color = if (state.isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    ProgressRing(progress = state.progress, modifier = Modifier.size(size), color = color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TimeContent(
                secondsFormatted = state.time,
                fontSize = when {
                    size < 240.dp -> if (state.time.length > 5) 32.sp else 44.sp
                    else -> if (state.time.length > 5) 44.sp else 64.sp
                },
                color = color
            )
            TimerHintText(hint = state.hint)
        }
    }
}
