package com.baltajmn.flowtime.data.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IosTimeSourceTest {
    private val time = IosTimeSource()

    @Test
    fun `el arranque no cambia y el reloj desde el arranque avanza`() {
        val boot = time.bootCount()
        val start = time.elapsedMillis()
        assertTrue(boot > 0)
        assertEquals(boot, time.bootCount())
        assertTrue(start > 0)
        assertTrue(time.elapsedMillis() >= start)
        // Desde que arrancó el Mac del simulador: mucho menos que desde 1970.
        assertTrue(start < time.wallMillis() / 2)
    }
}
