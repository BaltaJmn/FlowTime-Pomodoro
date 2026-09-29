package com.baltajmn.flowtime.data.timer

import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

enum class TimerMode(val continueAfterBreakKey: SharedPreferencesItem) {
    POMODORO(SharedPreferencesItem.CONTINUE_AFTER_BREAK_POMODORO),
    FLOW_TIME(SharedPreferencesItem.CONTINUE_AFTER_BREAK_FLOW_TIME),
    PERCENTAGE(SharedPreferencesItem.CONTINUE_AFTER_BREAK_PERCENTAGE)
}

enum class Phase { IDLE, WORK, BREAK }

/**
 * La sesión tal como se guarda. El tiempo no se va sumando cada segundo: se calcula a partir del
 * ancla (cuándo empezó a contar la fase, o cuándo se reanudó) y de lo que ya llevaba contado.
 */
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
    val workStartedAt: Long = 0
) {
    val isActive: Boolean get() = phase != Phase.IDLE
    val isPaused: Boolean get() = isActive && !running
    val countsDown: Boolean get() = durationMillis > 0
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
    private val time: TimeSource
) {
    private val _state = MutableStateFlow(restore())
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private val _changes = MutableSharedFlow<PhaseChange>(extraBufferCapacity = 16)
    val changes: SharedFlow<PhaseChange> = _changes.asSharedFlow()

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

    @Synchronized
    fun start(mode: TimerMode) {
        sync()
        val current = _state.value
        if (current.phase == Phase.WORK) record(current, elapsedMillis(current))
        set(newWork(mode, overshoot = 0))
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
        if (s.phase == Phase.BREAK) change(s, newWork(s.mode, overshoot = 0), overshoot = 0)
    }

    /** Para la sesión. Lo trabajado hasta ahora se guarda. */
    @Synchronized
    fun stop() {
        sync()
        val s = _state.value
        if (s.phase == Phase.WORK) record(s, elapsedMillis(s))
        if (s.isActive) set(FocusState(mode = s.mode))
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

    private fun breakMillis(mode: TimerMode, workedMillis: Long): Long = when (mode) {
        TimerMode.POMODORO -> pomodoroRange().rest * MINUTE

        TimerMode.FLOW_TIME -> flowTimeBreakSeconds(
            workedSeconds = workedMillis / 1000,
            ranges = (
                dataProvider.getRangeModelList(SharedPreferencesItem.FLOW_TIME_RANGE)
                    ?: TimerDefaults.flowTimeRanges()
                ).withCumulativeTotals()
        ) * 1000

        TimerMode.PERCENTAGE -> percentageBreakSeconds(
            workedSeconds = workedMillis / 1000,
            percentage = TimerDefaults.percentage(dataProvider.getLong(SharedPreferencesItem.PERCENTAGE_RANGE))
        ) * 1000
    }

    private fun pomodoroRange() =
        dataProvider.getRangeModel(SharedPreferencesItem.POMODORO_RANGE) ?: TimerDefaults.pomodoro()

    private fun finishWork(s: FocusState, workedMillis: Long, overshoot: Long) {
        record(s, workedMillis)
        val breakMillis = breakMillis(s.mode, workedMillis)
        val next = if (breakMillis > 0) {
            anchored(
                FocusState(
                    mode = s.mode,
                    phase = Phase.BREAK,
                    running = true,
                    durationMillis = breakMillis,
                    workStartedAt = s.workStartedAt
                ),
                overshoot
            )
        } else {
            afterBreak(s.mode, overshoot)
        }
        change(s, next, overshoot)
    }

    private fun finishBreak(s: FocusState, overshoot: Long) = change(s, afterBreak(s.mode, overshoot), overshoot)

    private fun afterBreak(mode: TimerMode, overshoot: Long) =
        if (dataProvider.getCheckValue(mode.continueAfterBreakKey)) newWork(mode, overshoot) else FocusState(mode = mode)

    private fun newWork(mode: TimerMode, overshoot: Long) = anchored(
        FocusState(
            mode = mode,
            phase = Phase.WORK,
            running = true,
            durationMillis = workMillis(mode),
            workStartedAt = time.wallMillis() - overshoot
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

    private fun record(s: FocusState, workedMillis: Long) {
        if (workedMillis >= MINUTE) dataProvider.updateMinutes(workedMillis / MINUTE)
    }

    private fun change(from: FocusState, to: FocusState, overshoot: Long) {
        set(to)
        _changes.tryEmit(PhaseChange(from.mode, from.phase, to.phase, overshoot))
    }

    private fun set(s: FocusState) {
        _state.value = s
        dataProvider.setObject(SharedPreferencesItem.TIMER_SESSION, s)
    }

    // Un estado a medias (de otra versión, o un fichero dañado) no puede impedir que la app arranque.
    private fun restore(): FocusState = runCatching {
        dataProvider.getObject(SharedPreferencesItem.TIMER_SESSION, FocusState::class.java)
            ?.takeIf { it.mode.name.isNotEmpty() && it.phase.name.isNotEmpty() }
    }.getOrNull() ?: FocusState()

    private companion object {
        const val MINUTE = 60_000L
    }
}
