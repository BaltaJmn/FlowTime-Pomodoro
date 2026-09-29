package com.baltajmn.flowtime.data.timer

import com.baltajmn.flowtime.core.persistence.model.RangeModel

/** Lo que enseña el anillo del temporizador: cuánto lleva, de 0 a 1, y una pista debajo. */
data class TimerProgress(val progress: Float, val hint: TimerHint? = null)

sealed interface TimerHint {
    /** FlowTime: a los [atMinutes] de trabajo, el descanso pasa a ser de [breakMinutes]. */
    data class NextStep(val atMinutes: Long, val breakMinutes: Long) : TimerHint

    /** Porcentaje: el descanso ganado hasta ahora. */
    data class Earned(val breakSeconds: Long) : TimerHint
}

/**
 * El anillo en cada modo: con cuenta atrás (Pomodoro y todos los descansos) se vacía; FlowTime
 * trabajando se llena hasta el siguiente tramo; Porcentaje trabajando da una vuelta por hora.
 */
fun timerProgress(snapshot: FocusSnapshot, flowTimeRanges: List<RangeModel>, percentage: Long): TimerProgress {
    val state = snapshot.state
    val workedSeconds = snapshot.elapsedMillis / 1000
    return when {
        // Parado, el Pomodoro se ve lleno, listo para empezar; los otros, vacíos.
        !state.isActive -> TimerProgress(if (state.mode == TimerMode.POMODORO) 1f else 0f)
        state.countsDown -> TimerProgress(snapshot.remainingMillis.toFloat() / state.durationMillis)
        state.mode == TimerMode.FLOW_TIME -> {
            val step = flowTimeNextStep(workedSeconds, flowTimeRanges) ?: return TimerProgress(1f)
            TimerProgress(
                progress = (workedSeconds - step.fromSeconds).toFloat() / (step.atSeconds - step.fromSeconds),
                hint = TimerHint.NextStep(step.atSeconds / 60, step.breakSeconds / 60)
            )
        }
        state.mode == TimerMode.PERCENTAGE -> TimerProgress(
            progress = (snapshot.elapsedMillis % HOUR).toFloat() / HOUR,
            hint = TimerHint.Earned(percentageBreakSeconds(workedSeconds, percentage))
        )
        else -> TimerProgress(0f)
    }
}

private const val HOUR = 60 * 60 * 1000L
