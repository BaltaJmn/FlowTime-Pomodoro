package com.baltajmn.flowtime.features.screens.focus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
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
import com.baltajmn.flowtime.features.screens.common.composable.components.ScreenTitle
import com.baltajmn.flowtime.features.screens.common.composable.components.TagChips
import com.baltajmn.flowtime.features.screens.common.composable.components.TaskChip
import com.baltajmn.flowtime.features.screens.common.composable.components.TaskDoneQuestion
import com.baltajmn.flowtime.features.screens.common.composable.components.TimeContent
import com.baltajmn.flowtime.features.screens.common.composable.components.TimerHintText
import com.baltajmn.flowtime.features.screens.common.composable.components.advantages
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import com.baltajmn.flowtime.features.screens.edit.ModeSettingsSheet
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private enum class FocusSheet { SOUNDS, MODE }

/** La pantalla de inicio (#51): el modo, el temporizador, empezar y el progreso de hoy. */
@Composable
fun FocusScreen(
    showSound: Boolean,
    viewModel: FocusViewModel = koinViewModel(),
    ambience: Ambience = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sound by ambience.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var explainNotifications by remember { mutableStateOf(false) }
    val askNotifications = rememberLauncherForActivityResult(RequestPermission()) {}
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
            if (action == TimerAction.START && !notificationsAllowed(context)) {
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
        FocusSheet.SOUNDS -> SoundSheet(onDismiss = { sheet = null })
        FocusSheet.MODE -> ModeSettingsSheet(mode = state.mode, onDismiss = { sheet = null })
        null -> Unit
    }

    if (explainNotifications) {
        AlertDialog(
            onDismissRequest = { explainNotifications = false },
            title = { Text(stringResource(R.string.notifications_title)) },
            text = { Text(stringResource(R.string.notifications_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explainNotifications = false
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) { Text(stringResource(R.string.turn_on)) }
            },
            dismissButton = {
                TextButton(onClick = { explainNotifications = false }) {
                    Text(stringResource(R.string.notifications_later))
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
                    painter = painterResource(R.drawable.ic_tune),
                    contentDescription = stringResource(R.string.cd_mode_settings),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    val today = @Composable {
        MinutesStudying(minutesStudying = state.minutesToday, goal = state.goalToday)
        if (state.streak > 0) {
            Text(
                text = pluralStringResource(R.plurals.streak_days, state.streak, state.streak),
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

    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    if (landscape) {
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
                Ring(state, size = 200.dp)
            }
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.5f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                tools()
                ModeSelector(state, onSelectMode)
                ModeLine(state)
                Spacer(modifier = Modifier.height(16.dp))
                ButtonsContent(state = state, onAction = onAction)
                Spacer(modifier = Modifier.height(16.dp))
                today()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
            Ring(state, size = 280.dp)
            Spacer(modifier = Modifier.height(32.dp))
            ButtonsContent(state = state, onAction = onAction)
            Spacer(modifier = Modifier.height(32.dp))
            today()
            Spacer(modifier = Modifier.height(24.dp))
        }
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
            if (state.isActive) R.string.focus_mode_locked else state.mode.advantages
        ),
        modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp),
        textAlign = TextAlign.Center,
        style = SubBody.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    )
}

/** Trabajando, descansando o en pausa. Parado no hace falta: ya se ve el modo. */
@Composable
private fun PhaseTitle(state: FocusUiState) {
    val title = when {
        state.paused -> R.string.time_title_paused
        state.phase == Phase.WORK -> R.string.time_title_working
        state.phase == Phase.BREAK -> R.string.time_title_resting
        else -> null
    }
    if (title != null) {
        ScreenTitle(text = stringResource(title))
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

// Antes de Android 13 no hay permiso que pedir; ContextCompat lo resuelve con los ajustes de la app.
private fun notificationsAllowed(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED

/** Solo mientras cuenta y esta pantalla está a la vista: ya no hace falta para no perder el tiempo. */
@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}
