package com.baltajmn.flowtime.core.design.sound

import com.baltajmn.flowtime.core.design.service.PlayerType
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class AmbientsTest {

    @Test
    fun `cada sonido se oye parecido de fuerte y no satura`() {
        PlayerType.entries.forEach { type ->
            val (rms, peak) = measure(ambientFor(type), seconds = 60)
            println("$type rms=${"%.3f".format(rms)} pico=${"%.2f".format(peak)}")
            assertTrue("$type se oye demasiado flojo o fuerte: rms $rms", rms in 0.025..0.2)
            assertTrue("$type satura: pico $peak", peak < 1.2f)
        }
    }

    private fun measure(ambient: Ambient, seconds: Int): Pair<Double, Float> {
        val left = FloatArray(BLOCK)
        val right = FloatArray(BLOCK)
        var sum = 0.0
        var peak = 0f
        val blocks = seconds * SAMPLE_RATE / BLOCK
        repeat(blocks) {
            ambient.render(left, right, BLOCK)
            for (i in 0 until BLOCK) {
                assertTrue(left[i].isFinite() && right[i].isFinite())
                sum += left[i] * left[i] + right[i] * right[i]
                peak = max(peak, max(abs(left[i]), abs(right[i])))
            }
        }
        return sqrt(sum / (2.0 * blocks * BLOCK)) to peak
    }

    private companion object {
        const val BLOCK = 1024
    }
}
