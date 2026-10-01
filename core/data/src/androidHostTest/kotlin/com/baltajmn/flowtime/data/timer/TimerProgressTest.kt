package com.baltajmn.flowtime.data.timer

import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerProgressTest {

    private val ranges = TimerDefaults.flowTimeRanges().withCumulativeTotals()

    private fun minutes(value: Int) = value * 60_000L

    private fun snapshot(mode: TimerMode, phase: Phase, elapsed: Long, duration: Long = 0) =
        FocusSnapshot(FocusState(mode = mode, phase = phase, running = true, durationMillis = duration), elapsed)

    @Test
    fun `con cuenta atras el anillo se vacia`() {
        val pomodoro = snapshot(TimerMode.POMODORO, Phase.WORK, elapsed = minutes(15), duration = minutes(60))
        val rest = snapshot(TimerMode.FLOW_TIME, Phase.BREAK, elapsed = minutes(9), duration = minutes(10))

        assertEquals(TimerProgress(0.75f), timerProgress(pomodoro, ranges, percentage = 20))
        assertEquals(TimerProgress(0.1f), timerProgress(rest, ranges, percentage = 20))
    }

    @Test
    fun `FlowTime se llena hasta el siguiente tramo y dice que descanso da`() {
        // A los 20 minutos, a medio camino del tramo de 15 a 30.
        val progress = timerProgress(snapshot(TimerMode.FLOW_TIME, Phase.WORK, minutes(20)), ranges, 20)

        assertEquals(1f / 3, progress.progress, 0.001f)
        assertEquals(TimerHint.NextStep(atMinutes = 30, breakMinutes = 15), progress.hint)
    }

    @Test
    fun `FlowTime tras el ultimo tramo se queda lleno y sin pista`() {
        assertEquals(TimerProgress(1f), timerProgress(snapshot(TimerMode.FLOW_TIME, Phase.WORK, minutes(50)), ranges, 20))
    }

    @Test
    fun `Porcentaje da una vuelta por hora y dice el descanso ganado`() {
        val progress = timerProgress(snapshot(TimerMode.PERCENTAGE, Phase.WORK, minutes(75)), ranges, 20)

        assertEquals(0.25f, progress.progress, 0.001f)
        assertEquals(TimerHint.Earned(breakSeconds = 15 * 60), progress.hint)
    }

    @Test
    fun `parado el Pomodoro se ve lleno y los demas vacios`() {
        assertEquals(1f, timerProgress(FocusSnapshot(FocusState(TimerMode.POMODORO), 0), ranges, 20).progress)
        assertEquals(0f, timerProgress(FocusSnapshot(FocusState(TimerMode.FLOW_TIME), 0), ranges, 20).progress)
    }
}
