package com.baltajmn.flowtime.features.screens.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
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
import com.baltajmn.flowtime.data.timer.FocusEngine
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
import com.baltajmn.flowtime.features.screens.platform.notificationsText
import com.baltajmn.flowtime.features.screens.platform.notificationsTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import com.baltajmn.flowtime.features.screens.platform.rememberNotificationPermission
import com.baltajmn.flowtime.features.screens.platform.KeepScreenOn

private enum class FocusSheet { SOUNDS, MODE }

/** Si hay una sesión en marcha: la pestaña de Enfoque lo marca desde cualquier otra pantalla. */
@Composable
fun sessionRunning(engine: FocusEngine = koinInject()): Boolean =
    engine.state.collectAsStateWithLifecycle().value.isActive

/** La pantalla de inicio (#51): el modo, el temporizador, empezar y el progreso de hoy. */
@Composable
fun FocusScreen(
    showSound: Boolean,
    viewModel: FocusViewModel = koinViewModel(),
    ambience: Ambience = koinInject(),
    gate: ProGate = koinInject(),
    proLauncher: ProLauncher = koinInject(),
    /** Lo de hoy en Estadísticas, para ver o corregir sus sesiones. */
    onOpenToday: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sound by ambience.state.collectAsStateWithLifecycle()
    val notifications = rememberNotificationPermission()
    var explainNotifications by remember { mutableStateOf(false) }
    var sheet by rememberSaveable { mutableStateOf<FocusSheet?>(null) }

    KeepScreenOn(active = state.keepScreenOn && state.isActive && !state.paused)

    // Parar no pregunta: dice lo que se ha guardado y deja deshacerlo, como borrar una tarea.
    val snackbar = remember { SnackbarHostState() }
    state.stoppedMillis?.let { worked ->
        val message = if (worked >= MINUTE_MILLIS) {
            stringResource(Res.string.stop_saved, (worked / MINUTE_MILLIS).formatMinutesStudying())
        } else {
            stringResource(Res.string.stop_too_short)
        }
        val undo = stringResource(Res.string.task_undo)
        // Más que el de borrar una tarea: parar sin querer es lo que más cuesta deshacer.
        LaunchedEffect(worked) {
            val result = snackbar.showSnackbar(message, undo, duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.undoStop() else viewModel.onStoppedShown()
        }
    }
    // Al irse de la pantalla el aviso se pierde: deshacer ya no vale, y al volver no debe salir otra vez.
    DisposableEffect(Unit) { onDispose { viewModel.onStoppedShown() } }

    Box(modifier = Modifier.fillMaxSize()) {
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
            onTaskNotYet = viewModel::keepTask,
            onCreateTask = { title -> viewModel.addTask(title) { proLauncher.open(ProFeature.TASKS) } },
            onOpenToday = onOpenToday
        )
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

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
            title = { Text(stringResource(notificationsTitle)) },
            text = { Text(stringResource(notificationsText)) },
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
    onTaskNotYet: () -> Unit = {},
    onCreateTask: ((String) -> Unit)? = null,
    onOpenToday: () -> Unit = {}
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
    // Antes de los botones, en qué se trabaja; después, lo que se lleva hoy. Lo que se elige va
    // delante de la acción, y el resultado detrás.
    // Con la sesión en marcha, la tarea y la etiqueta caben en una línea: tocarla las vuelve a abrir.
    val setup = @Composable {
        var open by rememberSaveable(state.isActive) { mutableStateOf(!state.isActive) }
        if (open) {
            TaskChip(
                title = state.taskTitle,
                pending = state.pendingTasks,
                onSelect = onTaskSelected,
                onCreate = onCreateTask
            )
        } else {
            SessionLine(
                task = state.taskTitle,
                tag = state.tags.firstOrNull { it.id == state.tagId }?.name,
                onClick = { open = true }
            )
        }
        if (state.askTaskDone && state.taskTitle != null) {
            TaskDoneQuestion(title = state.taskTitle, onYes = onTaskDone, onNotYet = onTaskNotYet)
        }
        if (open) TagChips(tags = state.tags, selected = state.tagId, onSelect = onTagSelected)
    }
    // Lo de hoy lleva a sus sesiones en Estadísticas; la racha, solo con la sesión parada.
    val today = @Composable {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(onClickLabel = stringResource(Res.string.cd_open_today), onClick = onOpenToday)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            MinutesStudying(minutesStudying = state.minutesToday, goal = state.goalToday)
        }
        if (state.streak > 0 && !state.isActive) {
            Text(
                text = pluralStringResource(Res.plurals.streak_days, state.streak, state.streak),
                style = SubBody.copy(
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }

    // Por el espacio que queda de verdad y no por la orientación del aparato (#46): en pantalla
    // dividida o en un plegable, la orientación no dice cuánto sitio hay.
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth > maxHeight) {
            val ring = minOf(maxHeight * 0.6f, maxWidth / 2 - 40.dp, 320.dp)
            TwoColumns(state, ring, onAction, onSelectMode, tools, setup, today)
        } else {
            OneColumn(
                state,
                ring = minOf(maxWidth - 32.dp, 280.dp),
                height = maxHeight,
                onAction,
                onSelectMode,
                tools,
                setup,
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
    setup: @Composable () -> Unit,
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
                if (!state.isActive) {
                    ModeSelector(state, onSelectMode)
                    ModeLine(state)
                    Spacer(modifier = Modifier.height(16.dp))
                }
                setup()
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
    height: Dp,
    onAction: (TimerAction) -> Unit,
    onSelectMode: (TimerMode) -> Unit,
    tools: @Composable () -> Unit,
    setup: @Composable () -> Unit,
    today: @Composable () -> Unit
) {
    RingColumn(
        space = height - WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        maxRing = ring,
        modifier = Modifier
            .fillMaxSize()
            .readableWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        // En marcha no se puede cambiar de modo: el selector no se enseña, y el modo va con la fase.
        top = {
            tools()
            if (!state.isActive) {
                ModeSelector(state, onSelectMode)
                ModeLine(state)
            }
            Spacer(modifier = Modifier.height(if (state.isActive) 8.dp else 24.dp))
            PhaseTitle(state)
        },
        ring = { size -> Ring(state, size) },
        bottom = {
            Spacer(modifier = Modifier.height(24.dp))
            setup()
            Spacer(modifier = Modifier.height(16.dp))
            ButtonsContent(state = state, onAction = onAction)
            Spacer(modifier = Modifier.height(16.dp))
            today()
            Spacer(modifier = Modifier.height(24.dp))
        }
    )
}

/**
 * Una columna centrada en la que el anillo cede alto para que todo quepa en [space] sin scroll: en el
 * iPhone, la isla y la barra de abajo con el indicador de inicio dejan menos sitio que en Android, y la
 * tarea quedaba debajo de la barra. Por debajo de [MIN_RING] el anillo ya no encoge y queda el scroll.
 */
@Composable
private fun RingColumn(
    space: Dp,
    maxRing: Dp,
    modifier: Modifier,
    top: @Composable () -> Unit,
    ring: @Composable (Dp) -> Unit,
    bottom: @Composable () -> Unit
) = SubcomposeLayout(modifier) { constraints ->
    val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
    val above = subcompose("top", top).map { it.measure(loose) }
    val below = subcompose("bottom", bottom).map { it.measure(loose) }
    val free = space.roundToPx() - (above + below).sumOf { it.height }
    val size = free.coerceIn(minOf(MIN_RING, maxRing).roundToPx(), maxRing.roundToPx()).toDp()
    val rows = above + subcompose("ring") { ring(size) }.map { it.measure(loose) } + below
    layout(constraints.maxWidth, rows.sumOf { it.height }.coerceAtLeast(constraints.minHeight)) {
        var y = 0
        rows.forEach {
            it.place(Alignment.CenterHorizontally.align(it.width, constraints.maxWidth, layoutDirection), y)
            y += it.height
        }
    }
}

/** Tarea y etiqueta de la sesión en marcha, en voz baja. */
@Composable
private fun SessionLine(task: String?, tag: String?, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
    ) {
        Text(
            text = listOfNotNull(tag, task).joinToString(" · ").ifEmpty { stringResource(Res.string.task_pick_title) },
            modifier = Modifier.widthIn(max = 280.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
    }
}

private val MIN_RING = 200.dp

private const val MINUTE_MILLIS = 60_000L

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
        text = stringResource(state.mode.advantages),
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
            text = stringResource(state.mode.label),
            style = SubBody.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        // Se anuncia al cambiar: de trabajar a descansar, sin mirar.
        Text(
            text = stringResource(title),
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun Ring(state: FocusUiState, size: Dp) {
    // Trabajo con primary y descanso con tertiary: se distinguen sin leer nada.
    val color = if (state.isBreak) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    // El reloj se mide por el anillo, no por la letra del sistema: con la letra al 200 % se salía, y
    // un anillo de 238 pt (el del iPhone) bajaba de golpe al tamaño pequeño. 64 sp en 280 dp.
    val clock = with(LocalDensity.current) { (size * if (state.time.length > 5) 0.16f else 0.23f).toSp() }
    ProgressRing(progress = state.progress, modifier = Modifier.size(size), color = color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TimeContent(secondsFormatted = state.time, fontSize = clock, color = color)
            // En el descanso, lo guardado del bloque: al terminar solo no había otro acuse que el color.
            if (state.isBreak && state.savedMillis >= MINUTE_MILLIS) {
                Text(
                    text = stringResource(Res.string.stop_saved, (state.savedMillis / MINUTE_MILLIS).formatMinutesStudying()),
                    modifier = Modifier.width(size * 0.7f),
                    style = SubBody.copy(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            } else {
                TimerHintText(hint = state.hint, modifier = Modifier.width(size * 0.7f))
            }
        }
    }
}
