package com.baltajmn.flowtime.data.timer

import androidx.annotation.StringRes
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.core.persistence.sharedpreferences.getObject
import com.baltajmn.flowtime.core.persistence.sharedpreferences.setObject
import com.baltajmn.flowtime.data.repository.SessionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

enum class TimerMode(val continueAfterBreakKey: SharedPreferencesItem) {
    POMODORO(SharedPreferencesItem.CONTINUE_AFTER_BREAK_POMODORO),
    FLOW_TIME(SharedPreferencesItem.CONTINUE_AFTER_BREAK_FLOW_TIME),
    PERCENTAGE(SharedPreferencesItem.CONTINUE_AFTER_BREAK_PERCENTAGE)
}

enum class Phase { IDLE, WORK, BREAK }

enum class TimerAction(@StringRes val label: Int) {
    START(R.string.action_start),
    PAUSE(R.string.action_pause),
    RESUME(R.string.action_resume),
    BREAK(R.string.action_break),
    SKIP_BREAK(R.string.action_skip_break),
    STOP(R.string.action_stop)
}

/**
 * Lo que se puede hacer con una sesión, en el orden en que se enseña, igual en la app que en la
 * notificación. En Pomodoro el trabajo termina solo; en los otros modos el descanso se pide.
 */
fun actionsFor(mode: TimerMode, phase: Phase, paused: Boolean): List<TimerAction> = when {
    phase == Phase.IDLE -> listOf(TimerAction.START)
    paused -> listOf(TimerAction.STOP, TimerAction.RESUME)
    phase == Phase.BREAK -> listOf(TimerAction.STOP, TimerAction.PAUSE, TimerAction.SKIP_BREAK)
    mode != TimerMode.POMODORO -> listOf(TimerAction.STOP, TimerAction.PAUSE, TimerAction.BREAK)
    else -> listOf(TimerAction.STOP, TimerAction.PAUSE)
}

/**
 * La sesión tal como se guarda. El tiempo no se va sumando cada segundo: se calcula a partir del
 * ancla (cuándo empezó a contar la fase, o cuándo se reanudó) y de lo que ya llevaba contado.
 */
@Serializable
data class FocusState(
    val mode: TimerMode = TimerMode.POMODORO,
    val phase: Phase = Phase.IDLE,
    val running: Boolean = false,
    val countedMillis: Long = 0,
    val anchorWall: Long = 0,
    val anchorElapsed: Long = 0,
    val anchorBoot: Int = 0,
    /** Duración de una fase con cuenta atrás; 0 si cuenta hacia arriba. */
    val durationMillis: Long = 0,
    /** Hora real a la que empezó el trabajo en curso, o el de antes de este descanso. */
    val workStartedAt: Long = 0,
    /** La etiqueta con la que se guarda el trabajo. Pasa de una fase a otra y a la sesión siguiente. */
    val tagId: Long? = null,
    /** La tarea en la que se trabaja. Como la etiqueta, sigue hasta que se cambie o se complete. */
    val taskId: Long? = null,
    /** Su título, para la notificación y el aviso, que no pueden esperar a la base de datos. */
    val taskTitle: String? = null
) {
    val isActive: Boolean get() = phase != Phase.IDLE
    val isPaused: Boolean get() = isActive && !running
    val countsDown: Boolean get() = durationMillis > 0
    val actions: List<TimerAction> get() = actionsFor(mode, phase, isPaused)
}

/** La sesión en un instante: lo que enseña la pantalla. */
data class FocusSnapshot(val state: FocusState, val elapsedMillis: Long) {
    val remainingMillis: Long get() = (state.durationMillis - elapsedMillis).coerceAtLeast(0)

    /** Segundos a mostrar: los que quedan, redondeando hacia arriba, o los que lleva. */
    val displaySeconds: Long
        get() = if (state.countsDown) (remainingMillis + 999) / 1000 else elapsedMillis / 1000
}

/** Un cambio de fase. [lateMillis] es cuánto después de ocurrir se ha visto: con la app cerrada, mucho. */
data class PhaseChange(val mode: TimerMode, val from: Phase, val to: Phase, val lateMillis: Long)

/**
 * El temporizador de toda la app, fuera de las pantallas: navegar, apagar la pantalla o que el
 * sistema cierre la app no lo para. Al volver se reconstruye con la hora y, si alguna fase terminó
 * mientras tanto, pasa a la que toque guardando lo trabajado.
 */
class FocusEngine(
    private val dataProvider: DataProvider,
    private val time: TimeSource,
    private val sessions: SessionRepository
) {
    private val _state = MutableStateFlow(restore())
    val state: StateFlow<FocusState> = _state.asStateFlow()

    /**
     * Se llama en cada cambio de fase, en el mismo hilo y al momento, también en los que se ven tarde
     * al volver a la app. Síncrono a propósito: con la app cerrada, quien despierta al motor es una
     * alarma, y el aviso tiene que salir antes de que el sistema vuelva a dormir el proceso.
     */
    var onPhaseChange: (PhaseChange) -> Unit = {}

    fun snapshot(state: FocusState = _state.value) = FocusSnapshot(state, elapsedMillis(state))

    /** Una instantánea al cambiar el estado y, mientras cuenta, otra en cada cambio de segundo. */
    fun snapshots(): Flow<FocusSnapshot> = state.transformLatest { s ->
        while (true) {
            val snapshot = snapshot(s)
            emit(snapshot)
            if (!s.running) break
            delay(1000 - snapshot.elapsedMillis % 1000)
            sync()
        }
    }

    /** Mientras el proceso vive, cambia de fase justo a su hora aunque no haya ninguna pantalla abierta. */
    fun runIn(scope: CoroutineScope) {
        scope.launch {
            state.collectLatest { s ->
                while (s.running && s.countsDown) {
                    delay(snapshot(s).remainingMillis.coerceAtLeast(1))
                    sync()
                }
            }
        }
    }

    /** [mode] solo cuenta al empezar: el resto de acciones son sobre la sesión en marcha. */
    fun perform(action: TimerAction, mode: TimerMode = _state.value.mode) = when (action) {
        TimerAction.START -> start(mode)
        TimerAction.PAUSE -> pause()
        TimerAction.RESUME -> resume()
        TimerAction.BREAK -> takeBreak()
        TimerAction.SKIP_BREAK -> skipBreak()
        TimerAction.STOP -> stop()
    }

    @Synchronized
    fun start(mode: TimerMode) {
        sync()
        val current = _state.value
        if (current.phase == Phase.WORK) record(current, elapsedMillis(current), overshoot = 0)
        set(newWork(mode, overshoot = 0, current))
    }

    /**
     * Elige el modo con la sesión parada: es el que se ve al abrir la app y el que empieza, y se guarda
     * como el último usado (#51). Con una sesión en marcha no hace nada: su modo no se cambia.
     */
    @Synchronized
    fun select(mode: TimerMode) {
        val s = _state.value
        if (!s.isActive && s.mode != mode) set(s.copy(mode = mode))
    }

    /** Con la sesión en marcha, cambia la del trabajo en curso, que es el que se guarda al terminar. */
    @Synchronized
    fun setTag(id: Long?) {
        dataProvider.setLong(SharedPreferencesItem.LAST_TAG_ID, id ?: NO_TAG)
        set(_state.value.copy(tagId = id))
    }

    /**
     * La tarea del trabajo en curso, o de la siguiente sesión si está parada. Si la tarea tiene
     * etiqueta ([tagId]), la sesión se queda con ella; si no, con la que tuviera.
     */
    @Synchronized
    fun setTask(id: Long?, tagId: Long? = null, title: String? = null) {
        if (tagId != null) setTag(tagId)
        set(_state.value.copy(taskId = id, taskTitle = title?.takeIf { id != null }))
    }

    @Synchronized
    fun pause() {
        sync()
        val s = _state.value
        if (s.isActive && s.running) set(s.copy(running = false, countedMillis = elapsedMillis(s)))
    }

    @Synchronized
    fun resume() {
        val s = _state.value
        if (s.isPaused) set(anchored(s.copy(running = true), overshoot = 0))
    }

    /** Termina el trabajo ahora y empieza el descanso que le toca. */
    @Synchronized
    fun takeBreak() {
        sync()
        val s = _state.value
        if (s.phase == Phase.WORK) finishWork(s, elapsedMillis(s), overshoot = 0)
    }

    @Synchronized
    fun skipBreak() {
        sync()
        val s = _state.value
        if (s.phase == Phase.BREAK) change(s, newWork(s.mode, overshoot = 0, s), overshoot = 0)
    }

    /** Para la sesión. Lo trabajado hasta ahora se guarda. */
    @Synchronized
    fun stop() {
        sync()
        val s = _state.value
        if (s.phase == Phase.WORK) record(s, elapsedMillis(s), overshoot = 0)
        if (s.isActive) set(s.stopped())
    }

    /** Pasa por las fases que hayan terminado desde la última vez, aunque la app estuviera cerrada. */
    @Synchronized
    fun sync() {
        while (true) {
            val s = _state.value
            if (!s.running || !s.countsDown) return
            val overshoot = elapsedMillis(s) - s.durationMillis
            if (overshoot < 0) return
            if (s.phase == Phase.WORK) finishWork(s, s.durationMillis, overshoot) else finishBreak(s, overshoot)
        }
    }

    /** Lo que dura el trabajo de un Pomodoro; 0 en los modos que cuentan hacia arriba. */
    fun workMillis(mode: TimerMode): Long =
        if (mode == TimerMode.POMODORO) pomodoroRange().endRange.coerceAtLeast(1) * MINUTE else 0

    /** Los tramos de FlowTime con sus totales acumulados: los mismos para el motor y para el anillo. */
    fun flowTimeRanges(): List<RangeModel> = (
        dataProvider.getObject<List<RangeModel>>(SharedPreferencesItem.FLOW_TIME_RANGE)
            ?: TimerDefaults.flowTimeRanges()
        ).withCumulativeTotals()

    fun percentage(): Long = TimerDefaults.percentage(dataProvider.getLong(SharedPreferencesItem.PERCENTAGE_RANGE))

    private fun breakMillis(mode: TimerMode, workedMillis: Long): Long = when (mode) {
        TimerMode.POMODORO -> pomodoroRange().rest * MINUTE
        TimerMode.FLOW_TIME -> flowTimeBreakSeconds(workedMillis / 1000, flowTimeRanges()) * 1000
        TimerMode.PERCENTAGE -> percentageBreakSeconds(workedMillis / 1000, percentage()) * 1000
    }

    private fun pomodoroRange() =
        dataProvider.getObject<RangeModel>(SharedPreferencesItem.POMODORO_RANGE) ?: TimerDefaults.pomodoro()

    private fun finishWork(s: FocusState, workedMillis: Long, overshoot: Long) {
        record(s, workedMillis, overshoot)
        val breakMillis = breakMillis(s.mode, workedMillis)
        val next = if (breakMillis > 0) {
            anchored(
                FocusState(
                    mode = s.mode,
                    phase = Phase.BREAK,
                    running = true,
                    durationMillis = breakMillis,
                    workStartedAt = s.workStartedAt,
                    tagId = s.tagId,
                    taskId = s.taskId,
                    taskTitle = s.taskTitle
                ),
                overshoot
            )
        } else {
            afterBreak(s, overshoot)
        }
        change(s, next, overshoot)
    }

    private fun finishBreak(s: FocusState, overshoot: Long) = change(s, afterBreak(s, overshoot), overshoot)

    private fun afterBreak(s: FocusState, overshoot: Long) =
        if (dataProvider.getCheckValue(s.mode.continueAfterBreakKey)) {
            newWork(s.mode, overshoot, s)
        } else {
            s.stopped()
        }

    /** Parada, con lo que se lleva a la sesión siguiente: el modo, la etiqueta y la tarea. */
    private fun FocusState.stopped() =
        FocusState(mode = mode, tagId = tagId, taskId = taskId, taskTitle = taskTitle)

    /** Un trabajo nuevo con la etiqueta y la tarea de [from]. */
    private fun newWork(mode: TimerMode, overshoot: Long, from: FocusState) = anchored(
        FocusState(
            mode = mode,
            phase = Phase.WORK,
            running = true,
            durationMillis = workMillis(mode),
            workStartedAt = time.wallMillis() - overshoot,
            tagId = from.tagId,
            taskId = from.taskId,
            taskTitle = from.taskTitle
        ),
        overshoot
    )

    /** La fase empezó a contar hace [overshoot]: cuando terminó la anterior, no cuando se ha visto. */
    private fun anchored(s: FocusState, overshoot: Long) = s.copy(
        anchorWall = time.wallMillis() - overshoot,
        anchorElapsed = time.elapsedMillis() - overshoot,
        anchorBoot = time.bootCount()
    )

    private fun elapsedMillis(s: FocusState): Long = s.countedMillis + if (s.running) sinceAnchor(s) else 0

    // Dentro del mismo arranque del móvil, el reloj que no cambia con la hora; tras reiniciar, la hora real.
    private fun sinceAnchor(s: FocusState): Long = if (time.bootCount() == s.anchorBoot) {
        time.elapsedMillis() - s.anchorElapsed
    } else {
        time.wallMillis() - s.anchorWall
    }.coerceAtLeast(0)

    // Con los segundos reales, sin las pausas. Menos de un minuto no se guarda: un empezar y parar
    // sin querer no es una sesión.
    private fun record(s: FocusState, workedMillis: Long, overshoot: Long) {
        if (workedMillis < MINUTE) return
        sessions.record(
            mode = s.mode.name,
            startedAt = s.workStartedAt,
            endedAt = time.wallMillis() - overshoot,
            focusSeconds = workedMillis / 1000,
            tagId = s.tagId,
            taskId = s.taskId
        )
    }

    private fun change(from: FocusState, to: FocusState, overshoot: Long) {
        set(to)
        onPhaseChange(PhaseChange(from.mode, from.phase, to.phase, overshoot))
    }

    private fun set(s: FocusState) {
        _state.value = s
        dataProvider.setObject(SharedPreferencesItem.TIMER_SESSION, s)
    }

    // Un estado a medias (de otra versión, o un fichero dañado) se lee como si no hubiera sesión. Sin
    // sesión guardada (la primera vez, o tras restaurar una copia en otro móvil), la etiqueta es la
    // última que se usó.
    private fun restore(): FocusState = dataProvider.getObject<FocusState>(SharedPreferencesItem.TIMER_SESSION)
        ?: FocusState(tagId = dataProvider.getLong(SharedPreferencesItem.LAST_TAG_ID).takeIf { it != NO_TAG })

    private companion object {
        const val MINUTE = 60_000L
        const val NO_TAG = 0L
    }
}
