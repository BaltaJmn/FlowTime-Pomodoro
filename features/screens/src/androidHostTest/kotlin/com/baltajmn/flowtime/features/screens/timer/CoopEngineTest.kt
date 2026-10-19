package com.baltajmn.flowtime.features.screens.timer

import com.baltajmn.flowtime.data.coop.CoopRoom
import com.baltajmn.flowtime.data.coop.clockOffset
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimeSource
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La sala manda en el motor: docs/coop.md 4. */
class CoopEngineTest {

    private class FakeTime : TimeSource {
        var wall = 1_790_000_000_000L
        var elapsed = 3_600_000L

        override fun wallMillis() = wall
        override fun elapsedMillis() = elapsed
        override fun bootCount() = 7

        fun advance(millis: Long) {
            wall += millis
            elapsed += millis
        }
    }

    private val time = FakeTime()
    private val sessions = FakeSessions()
    private val engine = FocusEngine(FakeDataProvider(), time, sessions)
    private val actions = mutableListOf<TimerAction>().also { list -> engine.coopControl = { list += it } }

    private fun minutes(value: Int) = value * 60_000L

    // El servidor va 3 s adelantado: la cuenta tiene que salir igual.
    private val offset = 3_000L
    private fun serverNow() = time.wall + offset

    // Una sala de 25 y 5 que empezó hace [ago] en la hora del servidor.
    private fun room(ago: Long, status: String = "running", counted: Long = 0) = CoopRoom(
        id = "r1",
        code = "ABCDEF",
        owner = "a",
        workMillis = minutes(25),
        breakMillis = minutes(5),
        status = status,
        countedMillis = counted,
        runningSince = if (status == "running") serverNow() - ago else null,
        serverNow = serverNow()
    )

    @Test
    fun `al entrar a mitad se ve lo que queda de la sala, con sus tiempos y no los de Ajustes`() {
        engine.follow(room(ago = minutes(20)), offset)

        val snapshot = engine.snapshot()
        assertEquals(Phase.WORK, snapshot.state.phase)
        assertEquals(5 * 60L, snapshot.displaySeconds)
    }

    @Test
    fun `quien entra a mitad guarda solo lo que estudio dentro, y el descanso es el de la sala`() {
        engine.follow(room(ago = minutes(20)), offset)

        time.advance(minutes(5) + 1_000)
        engine.sync()

        assertEquals(Phase.BREAK, engine.state.value.phase)
        assertEquals(minutes(5), engine.snapshot().state.durationMillis)
        assertEquals(listOf(5 * 60L), sessions.recorded.map { it.focusSeconds })
    }

    @Test
    fun `la sala sigue de ciclo en ciclo sola`() {
        engine.follow(room(ago = 0), offset)

        time.advance(minutes(30) + 1_000)
        engine.sync()

        assertEquals(Phase.WORK, engine.state.value.phase)
        assertEquals(listOf(25 * 60L), sessions.recorded.map { it.focusSeconds })
    }

    @Test
    fun `una pausa de otro no corta el trabajo, y parar lo guarda`() {
        engine.follow(room(ago = 0), offset)
        time.advance(minutes(10))

        engine.follow(room(ago = 0, status = "paused", counted = minutes(10)), offset)
        assertTrue(engine.state.value.isPaused)
        assertTrue(sessions.recorded.isEmpty())

        engine.follow(room(ago = 0, status = "idle"), offset)
        assertEquals(Phase.IDLE, engine.state.value.phase)
        assertEquals(listOf(10 * 60L), sessions.recorded.map { it.focusSeconds })
    }

    @Test
    fun `en la sala los botones van a la sala y no cambian nada aqui`() {
        engine.follow(room(ago = 0, status = "idle"), offset)

        engine.perform(TimerAction.START)
        assertEquals(0L, engine.stop())

        assertEquals(listOf(TimerAction.START, TimerAction.STOP), actions)
        assertEquals(Phase.IDLE, engine.state.value.phase)
    }

    @Test
    fun `salir guarda lo trabajado y vuelve a estudiar solo`() {
        engine.follow(room(ago = 0), offset)
        time.advance(minutes(3))

        engine.leaveCoop()

        assertNull(engine.state.value.coop)
        assertEquals(Phase.IDLE, engine.state.value.phase)
        assertEquals(listOf(3 * 60L), sessions.recorded.map { it.focusSeconds })
    }

    @Test
    fun `una sala olvidada mas de 8 ciclos se da por parada`() {
        assertEquals(Phase.IDLE, room(ago = minutes(8 * 30)).phaseAt(serverNow()).phase)
        assertEquals(Phase.BREAK, room(ago = minutes(7 * 30 + 26)).phaseAt(serverNow()).phase)
    }

    @Test
    fun `el desfase de reloj supone que el servidor contesto a mitad de camino`() {
        assertEquals(1_000L, clockOffset(serverNow = 11_000, sentAt = 9_000, receivedAt = 11_000))
    }
}
