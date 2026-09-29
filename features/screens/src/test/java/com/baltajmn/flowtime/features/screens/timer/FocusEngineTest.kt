package com.baltajmn.flowtime.features.screens.timer

import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.PhaseChange
import com.baltajmn.flowtime.data.timer.TimeSource
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate

class FocusEngineTest {

    private class FakeTime : TimeSource {
        var wall = 1_790_000_000_000L
        var elapsed = 3_600_000L
        var boot = 7

        override fun wallMillis() = wall
        override fun elapsedMillis() = elapsed
        override fun bootCount() = boot

        fun advance(millis: Long) {
            wall += millis
            elapsed += millis
        }
    }

    private val time = FakeTime()
    private val prefs = FakeDataProvider()
    private val engine = FocusEngine(prefs, time)

    private fun minutes(value: Int) = value * 60_000L
    private fun seconds(value: Int) = value * 1_000L
    private fun minutesToday() = prefs.getMinutesByDate(LocalDate.of(2026, 9, 29))

    // Por defecto: Pomodoro de 45 minutos con 15 de descanso.

    @Test
    fun `la cuenta atras redondea hacia arriba y baja un segundo por segundo`() {
        engine.start(TimerMode.POMODORO)
        assertEquals(45 * 60L, engine.snapshot().displaySeconds)

        time.advance(300)
        assertEquals(45 * 60L, engine.snapshot().displaySeconds)

        time.advance(700)
        assertEquals(45 * 60L - 1, engine.snapshot().displaySeconds)
    }

    @Test
    fun `con la pantalla apagada el pomodoro termina a su hora y guarda lo trabajado`() {
        engine.start(TimerMode.POMODORO)

        time.advance(minutes(45) + seconds(20))
        engine.sync()

        val state = engine.state.value
        assertEquals(Phase.BREAK, state.phase)
        assertEquals(minutes(15) - seconds(20), engine.snapshot().remainingMillis)
        assertEquals(45L, minutesToday())
    }

    @Test
    fun `al volver pasa por todas las fases que terminaron mientras tanto`() {
        engine.start(TimerMode.POMODORO)

        time.advance(minutes(45 + 15 + 10))
        engine.sync()

        assertEquals(Phase.WORK, engine.state.value.phase)
        assertEquals(minutes(10), engine.snapshot().elapsedMillis)
        assertEquals(45L, minutesToday())
    }

    @Test
    fun `sin continuar despues del descanso la sesion termina`() {
        prefs.setCheckValue(SharedPreferencesItem.CONTINUE_AFTER_BREAK_POMODORO, false)
        engine.start(TimerMode.POMODORO)

        time.advance(minutes(61))
        engine.sync()

        assertFalse(engine.state.value.isActive)
    }

    @Test
    fun `las pausas no cuentan`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(5))
        engine.pause()
        time.advance(minutes(60))
        engine.resume()
        time.advance(minutes(1))

        assertEquals(minutes(6), engine.snapshot().elapsedMillis)
    }

    @Test
    fun `una fase en pausa no termina aunque pase su hora`() {
        engine.start(TimerMode.POMODORO)
        time.advance(minutes(40))
        engine.pause()
        time.advance(minutes(30))
        engine.sync()

        assertEquals(Phase.WORK, engine.state.value.phase)
        assertEquals(minutes(5), engine.snapshot().remainingMillis)
    }

    @Test
    fun `si el sistema cierra la app, al abrirla sigue donde iba`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(10))

        val reopened = FocusEngine(prefs, time)

        assertEquals(Phase.WORK, reopened.state.value.phase)
        assertEquals(minutes(10), reopened.snapshot().elapsedMillis)
    }

    @Test
    fun `cambiar la hora del movil no cambia lo que lleva contado`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(10))

        time.wall += minutes(180)

        assertEquals(minutes(10), engine.snapshot().elapsedMillis)
        assertEquals(minutes(10), FocusEngine(prefs, time).snapshot().elapsedMillis)
    }

    @Test
    fun `tras reiniciar el movil cuenta con la hora real`() {
        engine.start(TimerMode.FLOW_TIME)
        time.wall += minutes(20)
        time.boot += 1
        time.elapsed = seconds(30)

        assertEquals(minutes(20), FocusEngine(prefs, time).snapshot().elapsedMillis)
    }

    @Test
    fun `el descanso de FlowTime depende de lo trabajado`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(20))

        engine.takeBreak()

        assertEquals(Phase.BREAK, engine.state.value.phase)
        assertEquals(minutes(10), engine.snapshot().remainingMillis)
        assertEquals(20L, minutesToday())
    }

    // Reseña de noviembre de 2024: el contador del modo Porcentaje corría demasiado rápido.
    @Test
    fun `empezar dos veces no acelera el contador`() {
        engine.start(TimerMode.PERCENTAGE)
        engine.start(TimerMode.PERCENTAGE)
        time.advance(seconds(10))

        assertEquals(10L, engine.snapshot().displaySeconds)
    }

    @Test
    fun `el descanso del modo Porcentaje dura su parte y despues se vuelve a trabajar`() {
        engine.start(TimerMode.PERCENTAGE)
        time.advance(seconds(50))

        engine.takeBreak()
        assertEquals(seconds(10), engine.snapshot().remainingMillis)

        time.advance(seconds(10))
        engine.sync()
        assertEquals(Phase.WORK, engine.state.value.phase)
    }

    @Test
    fun `saltar el descanso vuelve al trabajo`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(20))
        engine.takeBreak()

        engine.skipBreak()

        assertEquals(Phase.WORK, engine.state.value.phase)
        assertEquals(0L, engine.snapshot().elapsedMillis)
    }

    @Test
    fun `parar a medias guarda lo trabajado`() {
        engine.start(TimerMode.POMODORO)
        time.advance(minutes(24) + seconds(59))

        engine.stop()

        assertFalse(engine.state.value.isActive)
        assertEquals(24L, minutesToday())
    }

    @Test
    fun `empezar otro modo termina la sesion anterior guardandola`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(12))

        engine.start(TimerMode.POMODORO)

        assertEquals(TimerMode.POMODORO, engine.state.value.mode)
        assertEquals(12L, minutesToday())
    }

    @Test
    fun `un estado guardado que no se entiende no impide arrancar`() {
        prefs.setString(SharedPreferencesItem.TIMER_SESSION, "{roto")

        assertFalse(FocusEngine(prefs, time).state.value.isActive)
    }

    @Test
    fun `avisa del cambio de fase y de cuanto tarde se ha visto`() = runTest {
        val changes = mutableListOf<PhaseChange>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            engine.changes.toList(
                changes
            )
        }
        engine.start(TimerMode.POMODORO)
        time.advance(minutes(45) + seconds(5))

        engine.sync()

        assertEquals(
            listOf(PhaseChange(TimerMode.POMODORO, Phase.WORK, Phase.BREAK, seconds(5))),
            changes
        )
    }
}
