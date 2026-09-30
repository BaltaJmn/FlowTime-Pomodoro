package com.baltajmn.flowtime.data.timer

import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusModeTest {

    private class FakeDnd(override val hasRules: Boolean = true) : DoNotDisturb {
        override var granted = true
        var rule: Boolean? = null
        override var filter = ALL
        override fun setRule(active: Boolean) {
            rule = active
        }
    }

    private val prefs = FakeDataProvider()
    private val work = FocusState(phase = Phase.WORK, running = true)
    private val paused = work.copy(running = false)
    private val rest = FocusState(phase = Phase.BREAK, running = true)
    private val stopped = FocusState()

    private fun focusMode(dnd: DoNotDisturb, pro: Boolean = true, proEnabled: Boolean = true) =
        FocusMode(prefs, dnd, MutableStateFlow(pro), proEnabled).apply { setEnabled(true) }

    @Test
    fun `se activa en el trabajo, tambien en pausa, y se quita en el descanso y al parar`() {
        val dnd = FakeDnd()
        val mode = focusMode(dnd)

        mode.apply(work)
        assertEquals(true, dnd.rule)
        mode.apply(paused)
        assertEquals(true, dnd.rule)
        mode.apply(rest)
        assertEquals(false, dnd.rule)
        mode.apply(work)
        mode.apply(stopped)
        assertEquals(false, dnd.rule)
    }

    @Test
    fun `al arrancar con la sesion parada quita la regla que se quedo puesta`() {
        val dnd = FakeDnd().apply { rule = true }
        focusMode(dnd).apply(stopped)
        assertEquals(false, dnd.rule)
    }

    @Test
    fun `sin permiso no toca nada`() {
        val dnd = FakeDnd().apply { granted = false }
        focusMode(dnd).apply(work)
        assertNull(dnd.rule)
    }

    @Test
    fun `apagado, sin Pro o sin Pro a la venta no se activa`() {
        FakeDnd().let { dnd ->
            focusMode(dnd).apply { setEnabled(false) }.apply(work)
            assertEquals(false, dnd.rule)
        }
        FakeDnd().let { dnd ->
            focusMode(dnd, pro = false).apply(work)
            assertEquals(false, dnd.rule)
        }
        FakeDnd().let { dnd ->
            focusMode(dnd, proEnabled = false).apply(work)
            assertEquals(false, dnd.rule)
        }
    }

    @Test
    fun `sin reglas devuelve el filtro que habia, tambien si ya estaba en No molestar`() {
        val dnd = FakeDnd(hasRules = false).apply { filter = ALARMS }
        val mode = focusMode(dnd)

        mode.apply(work)
        assertEquals(DoNotDisturb.PRIORITY, dnd.filter)
        // Otra fase de trabajo seguida no pisa lo guardado.
        mode.apply(paused)
        mode.apply(rest)
        assertEquals(ALARMS, dnd.filter)

        // Sin nada guardado, parar no cambia lo que el usuario tenga puesto.
        dnd.filter = ALL
        mode.apply(stopped)
        assertEquals(ALL, dnd.filter)
    }

    @Test
    fun `el ajuste se guarda`() {
        focusMode(FakeDnd())
        assertTrue(FocusMode(prefs, FakeDnd(), MutableStateFlow(true)).enabled.value)
        FocusMode(prefs, FakeDnd(), MutableStateFlow(true)).setEnabled(false)
        assertFalse(FocusMode(prefs, FakeDnd(), MutableStateFlow(true)).enabled.value)
    }

    private companion object {
        const val ALL = 1
        const val ALARMS = 4
    }
}
