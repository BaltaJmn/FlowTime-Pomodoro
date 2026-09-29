package com.baltajmn.flowtime.features.screens.timer

import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.PhaseChange
import com.baltajmn.flowtime.data.timer.TimeSource
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

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
    private val sessions = FakeSessions()
    private val engine = FocusEngine(prefs, time, sessions)

    private fun minutes(value: Int) = value * 60_000L
    private fun seconds(value: Int) = value * 1_000L
    private fun secondsRecorded() = sessions.recorded.sumOf { it.focusSeconds }

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
        val start = time.wall
        engine.start(TimerMode.POMODORO)

        time.advance(minutes(45) + seconds(20))
        engine.sync()

        val state = engine.state.value
        assertEquals(Phase.BREAK, state.phase)
        assertEquals(minutes(15) - seconds(20), engine.snapshot().remainingMillis)
        // La sesión acaba cuando terminó la fase, no cuando se ha visto.
        assertEquals(
            FakeSessions.Recorded("POMODORO", start, start + minutes(45), 45 * 60L),
            sessions.recorded.single()
        )
    }

    @Test
    fun `al volver pasa por todas las fases que terminaron mientras tanto`() {
        engine.start(TimerMode.POMODORO)

        time.advance(minutes(45 + 15 + 10))
        engine.sync()

        assertEquals(Phase.WORK, engine.state.value.phase)
        assertEquals(minutes(10), engine.snapshot().elapsedMillis)
        assertEquals(45 * 60L, secondsRecorded())
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

        val reopened = FocusEngine(prefs, time, sessions)

        assertEquals(Phase.WORK, reopened.state.value.phase)
        assertEquals(minutes(10), reopened.snapshot().elapsedMillis)
    }

    @Test
    fun `cambiar la hora del movil no cambia lo que lleva contado`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(10))

        time.wall += minutes(180)

        assertEquals(minutes(10), engine.snapshot().elapsedMillis)
        assertEquals(minutes(10), FocusEngine(prefs, time, sessions).snapshot().elapsedMillis)
    }

    @Test
    fun `tras reiniciar el movil cuenta con la hora real`() {
        engine.start(TimerMode.FLOW_TIME)
        time.wall += minutes(20)
        time.boot += 1
        time.elapsed = seconds(30)

        assertEquals(minutes(20), FocusEngine(prefs, time, sessions).snapshot().elapsedMillis)
    }

    @Test
    fun `el descanso de FlowTime depende de lo trabajado`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(20))

        engine.takeBreak()

        assertEquals(Phase.BREAK, engine.state.value.phase)
        assertEquals(minutes(10), engine.snapshot().remainingMillis)
        assertEquals(20 * 60L, secondsRecorded())
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
    fun `parar a medias guarda lo trabajado con sus segundos`() {
        engine.start(TimerMode.POMODORO)
        time.advance(minutes(24) + seconds(59))

        engine.stop()

        assertFalse(engine.state.value.isActive)
        assertEquals(24 * 60L + 59, secondsRecorded())
    }

    @Test
    fun `menos de un minuto no es una sesion`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(seconds(59))

        engine.stop()

        assertEquals(emptyList<FakeSessions.Recorded>(), sessions.recorded)
    }

    @Test
    fun `empezar otro modo termina la sesion anterior guardandola`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(12))

        engine.start(TimerMode.POMODORO)

        assertEquals(TimerMode.POMODORO, engine.state.value.mode)
        assertEquals(12 * 60L, secondsRecorded())
    }

    @Test
    fun `un estado guardado que no se entiende no impide arrancar`() {
        prefs.setString(SharedPreferencesItem.TIMER_SESSION, "{roto")

        assertFalse(FocusEngine(prefs, time, sessions).state.value.isActive)
    }

    @Test
    fun `el bloque se guarda con la etiqueta que habia al terminar`() {
        engine.setTag(1)
        engine.start(TimerMode.POMODORO)
        time.advance(minutes(10))

        // Cambiarla a mitad de bloque cambia la del bloque en curso.
        engine.setTag(2)
        time.advance(minutes(35))
        engine.sync()
        // Y sigue en el siguiente, tras el descanso.
        time.advance(minutes(15 + 45))
        engine.sync()

        assertEquals(listOf(2L, 2L), sessions.recorded.map { it.tagId })
    }

    @Test
    fun `sin elegir etiqueta el bloque se guarda sin ella`() {
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(20))

        engine.stop()

        assertEquals(null, sessions.recorded.single().tagId)
    }

    @Test
    fun `la etiqueta se queda para la sesion siguiente, tambien al reabrir la app`() {
        engine.setTag(3)
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(20))
        engine.stop()

        assertEquals(3L, FocusEngine(prefs, time, sessions).state.value.tagId)
        // Sin la sesión guardada (una copia restaurada en otro móvil), la última que se usó.
        prefs.values.remove(SharedPreferencesItem.TIMER_SESSION.name.lowercase())
        assertEquals(3L, FocusEngine(prefs, time, sessions).state.value.tagId)
    }

    @Test
    fun `avisa del cambio de fase y de cuanto tarde se ha visto`() {
        val changes = mutableListOf<PhaseChange>()
        engine.onPhaseChange = { changes += it }
        engine.start(TimerMode.POMODORO)
        time.advance(minutes(45) + seconds(5))

        engine.sync()

        assertEquals(
            listOf(PhaseChange(TimerMode.POMODORO, Phase.WORK, Phase.BREAK, seconds(5))),
            changes
        )
    }
}
