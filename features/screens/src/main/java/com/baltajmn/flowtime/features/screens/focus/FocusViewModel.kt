package com.baltajmn.flowtime.features.screens.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.KEEP_SCREEN_ON
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.NOTIFICATIONS_EXPLAINED
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.task.TaskRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusSnapshot
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.data.timer.TimerHint
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.data.timer.actionsFor
import com.baltajmn.flowtime.data.timer.timerProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class FocusUiState(
    val mode: TimerMode = TimerMode.FLOW_TIME,
    val phase: Phase = Phase.IDLE,
    val paused: Boolean = false,
    val time: String = "00:00",
    val minutesToday: String = "",
    /** El objetivo de hoy, ya formateado. Vacío hasta que se lee. */
    val goalToday: String = "",
    /** Días seguidos cumpliendo el objetivo (#36). */
    val streak: Int = 0,
    val keepScreenOn: Boolean = true,
    /** Las etiquetas activas, para elegir con cuál se guarda el trabajo. */
    val tags: List<Tag> = emptyList(),
    val tagId: Long? = null,
    /** Lo que lleva el anillo, de 0 a 1. */
    val progress: Float = 0f,
    val hint: TimerHint? = null,
    /** La tarea en la que se trabaja (#40), y las pendientes de hoy para elegir otra. */
    val taskId: Long? = null,
    val taskTitle: String? = null,
    val pendingTasks: List<Task> = emptyList(),
    /** En el descanso de un bloque con tarea: preguntar si se ha terminado. */
    val askTaskDone: Boolean = false
) {
    val isActive get() = phase != Phase.IDLE
    val isBreak get() = phase == Phase.BREAK
    val actions get() = actionsFor(mode, phase, paused)
}

/**
 * La pantalla de concentración (#51): el modo es el del motor, que es también el de la sesión en
 * marcha si la hay. Solo se cambia con la sesión parada.
 */
class FocusViewModel(
    private val engine: FocusEngine,
    private val dataProvider: DataProvider,
    goals: GoalRepository,
    tags: TagRepository,
    private val tasks: TaskRepository
) : ViewModel() {

    private val keepScreenOn = dataProvider.getBoolean(KEEP_SCREEN_ON, true)

    // Las pendientes de hoy, y el descanso (por la hora de su trabajo) en el que ya se contestó.
    private val pendingTasks = LocalDate.now().let { today ->
        tasks.day(today, today).map { list -> list.filterNot(Task::done) }
    }
    private val answeredFor = MutableStateFlow<Long?>(null)

    // El día sale del repositorio del objetivo: con la pantalla abierta pasada la medianoche, antes
    // seguía sumando los minutos de ayer.
    val uiState: StateFlow<FocusUiState> =
        combine(
            engine.snapshots(),
            goals.today.onStart<DayProgress?> { emit(null) },
            goals.streak.map { it.current }.onStart { emit(0) },
            tags.active.onStart { emit(emptyList()) },
            ::toUiState
        ).combine(pendingTasks.onStart { emit(emptyList()) }) { state, pending ->
            state.copy(pendingTasks = pending)
        }.combine(answeredFor) { state, answered ->
            val asked = answered == engine.state.value.workStartedAt
            state.copy(askTaskDone = state.askTaskDone && !asked)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            toUiState(engine.snapshot(), today = null, streak = 0, tags = emptyList())
        )

    init {
        // Una etiqueta archivada deja de ser la elegida, salvo para el trabajo que ya está en marcha.
        viewModelScope.launch {
            combine(engine.state, tags.active, ::Pair).collect { (state, active) ->
                val tag = state.tagId ?: return@collect
                if (!state.isActive && active.none { it.id == tag }) engine.setTag(null)
            }
        }
    }

    fun onAction(action: TimerAction) = engine.perform(action)

    /** Con una sesión en marcha, el motor no lo cambia. */
    fun select(mode: TimerMode) = engine.select(mode)

    fun selectTag(id: Long?) = engine.setTag(id)

    /** Si la tarea tiene etiqueta, la sesión se queda con ella. */
    fun selectTask(task: Task?) = engine.setTask(task?.id, task?.tagId, task?.title)

    /** "Sí": se completa y deja de ser la de la sesión. */
    fun completeTask() {
        val id = engine.state.value.taskId ?: return
        viewModelScope.launch {
            tasks.setDone(id, done = true, today = LocalDate.now())
            engine.setTask(null)
        }
    }

    /** "Aún no": la tarea sigue, y no se vuelve a preguntar en este descanso. */
    fun keepTask() = answeredFor.update { engine.state.value.workStartedAt }

    /** El permiso de notificaciones se explica una sola vez: después, el aviso queda en Ajustes. */
    fun explainNotificationsOnce(): Boolean {
        if (dataProvider.getBoolean(NOTIFICATIONS_EXPLAINED, false)) return false
        dataProvider.setBoolean(NOTIFICATIONS_EXPLAINED, true)
        return true
    }

    private fun toUiState(
        snapshot: FocusSnapshot,
        today: DayProgress?,
        streak: Int,
        tags: List<Tag>
    ): FocusUiState {
        val session = snapshot.state
        val mode = session.mode
        // Parada, el anillo sale vacío aunque el motor guarde algo de la sesión anterior.
        val ring = timerProgress(
            snapshot = if (session.isActive) {
                snapshot
            } else {
                FocusSnapshot(
                    FocusState(mode),
                    elapsedMillis = 0
                )
            },
            flowTimeRanges = if (session.isActive && mode == TimerMode.FLOW_TIME) engine.flowTimeRanges() else emptyList(),
            percentage = engine.percentage()
        )
        return FocusUiState(
            mode = mode,
            phase = session.phase,
            paused = session.isPaused,
            time = (if (session.isActive) snapshot.displaySeconds else engine.workMillis(mode) / 1000).formatSecondsToTime(),
            minutesToday = ((today?.seconds ?: 0) / 60).formatMinutesStudying(),
            goalToday = today?.goalMinutes?.toLong()?.formatMinutesStudying().orEmpty(),
            streak = streak,
            keepScreenOn = keepScreenOn,
            tags = tags,
            tagId = session.tagId,
            progress = ring.progress,
            hint = ring.hint,
            taskId = session.taskId,
            taskTitle = session.taskTitle,
            askTaskDone = session.phase == Phase.BREAK && session.taskId != null
        )
    }
}
