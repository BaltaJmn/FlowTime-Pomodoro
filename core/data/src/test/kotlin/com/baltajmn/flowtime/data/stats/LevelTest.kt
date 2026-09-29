package com.baltajmn.flowtime.data.stats

import org.junit.Assert.assertEquals
import org.junit.Test

class LevelTest {

    /** El cálculo tal como estaba en SettingsViewModel, para comprobar que nadie cambia de nivel. */
    private fun before(minutes: Long): Level {
        val xpBase = 100
        val xpPerHour = 50
        val xpTotal = (minutes * xpPerHour).toInt()
        var level = 0
        var current = 0
        var next = xpBase
        while (xpTotal >= next) {
            level++
            current = next
            next = xpBase * (level + 1) * (level + 1)
        }
        val progress = ((xpTotal - current).toDouble() / (next - current)) * 100
        return Level(level.toLong(), progress.toLong())
    }

    @Test
    fun `el mismo nivel que antes para cualquier tiempo`() {
        // De 0 a unas 1.600 horas: más de lo que lleva nadie.
        (0L..100_000L step 7).forEach { minutes -> assertEquals("$minutes min", before(minutes), level(minutes)) }
    }

    @Test
    fun `los primeros niveles`() {
        assertEquals(Level(0, 0), level(0))
        assertEquals(Level(1, 0), level(2))
        assertEquals(Level(1, 50), level(5))
    }
}
