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
    fun `los cambios de antemano son los mismos que hace sync`() {
        engine.start(TimerMode.POMODORO)
        val upcoming = engine.upcomingChanges(count = 4)
        assertEquals(listOf(minutes(45), minutes(60), minutes(105), minutes(120)), upcoming.map { it.first })

        val seen = mutableListOf<PhaseChange>()
        engine.onPhaseChange = { seen += it }
        var now = 0L
        upcoming.forEach { (at, _) ->
            time.advance(at - now)
            now = at
            engine.sync()
        }
        assertEquals(upcoming.map { it.second }, seen)
    }

    @Test
    fun `sin seguir tras el descanso, el ultimo cambio de antemano es el final de la sesion`() {
        prefs.setCheckValue(TimerMode.POMODORO.continueAfterBreakKey, false)
        engine.start(TimerMode.POMODORO)

        assertEquals(listOf(Phase.BREAK, Phase.IDLE), engine.upcomingChanges(count = 8).map { it.second.to })
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
    fun `deshacer parar sigue contando como si no se hubiera parado y borra lo guardado`() {
        val start = time.wall
        engine.start(TimerMode.FLOW_TIME)
        time.advance(minutes(20))

        assertEquals(minutes(20), engine.stop())
        time.advance(seconds(3))
        engine.undoStop()

        assertEquals(Phase.WORK, engine.state.value.phase)
        assertEquals(minutes(20) + seconds(3), engine.snapshot().elapsedMillis)
        assertEquals(listOf(start), sessions.unrecorded)
        // Solo justo después: con otra sesión empezada, no hace nada.
        engine.stop()
        engine.start(TimerMode.POMODORO)
        engine.undoStop()
        assertEquals(TimerMode.POMODORO, engine.state.value.mode)
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
    fun `el bloque se guarda con su tarea, que sigue tras el descanso`() {
        engine.setTask(7)
        engine.start(TimerMode.POMODORO)

        time.advance(minutes(45 + 15 + 45))
        engine.sync()

        assertEquals(listOf(7L, 7L), sessions.recorded.map { it.taskId })
    }

    @Test
    fun `elegir una tarea con etiqueta pone esa etiqueta`() {
        engine.setTag(1)

        engine.setTask(7, tagId = 4)
        assertEquals(4L, engine.state.value.tagId)

        // Una tarea sin etiqueta deja la que hubiera.
        engine.setTask(8)
        assertEquals(4L, engine.state.value.tagId)
        assertEquals(8L, engine.state.value.taskId)
    }

    @Test
    fun `elegir modo con la sesion parada lo guarda como el ultimo usado`() {
        engine.select(TimerMode.PERCENTAGE)

        assertEquals(TimerMode.PERCENTAGE, engine.state.value.mode)
        assertEquals(TimerMode.PERCENTAGE, FocusEngine(prefs, time, sessions).state.value.mode)
    }

    @Test
    fun `con una sesion en marcha no se cambia de modo`() {
        engine.start(TimerMode.FLOW_TIME)

        engine.select(TimerMode.POMODORO)

        assertEquals(TimerMode.FLOW_TIME, engine.state.value.mode)
        assertEquals(Phase.WORK, engine.state.value.phase)
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
