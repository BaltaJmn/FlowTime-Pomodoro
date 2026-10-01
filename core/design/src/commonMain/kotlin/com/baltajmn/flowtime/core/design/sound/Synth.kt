package com.baltajmn.flowtime.core.design.sound

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.tan

// Piezas de sintesis para los sonidos ambientales. Todo se calcula muestra a muestra y sin reservar
// memoria dentro del bucle de audio.

internal const val SAMPLE_RATE = 48_000

private const val TWO_PI = 2f * PI.toFloat()

/** xorshift32: rapido y de sobra para ruido. Cada fuente lleva el suyo para que no se parezcan. */
internal class Rng(seed: Int) {
    private var state = seed or 1

    private fun nextInt(): Int {
        var x = state
        x = x xor (x shl 13)
        x = x xor (x ushr 17)
        x = x xor (x shl 5)
        state = x
        return x
    }

    /** Entre -1 y 1. */
    fun white(): Float = nextInt() * (1f / 2_147_483_648f)

    /** Entre 0 y 1. */
    fun unit(): Float = (nextInt() ushr 8) * (1f / 16_777_216f)

    fun range(from: Float, to: Float): Float = from + (to - from) * unit()

    /** Muestras hasta el siguiente de unos sucesos que llegan de media [perSecond] veces por segundo. */
    fun wait(perSecond: Float): Int = (-ln(1f - unit()) / perSecond * SAMPLE_RATE).toInt() + 1

    fun waitSeconds(from: Float, to: Float): Int = (range(from, to) * SAMPLE_RATE).toInt() + 1
}

internal object Sine {
    private const val SIZE = 4096
    private val table = FloatArray(SIZE + 1) { sin(2 * PI * it / SIZE).toFloat() }

    /** [phase] en vueltas: 0,25 es el maximo. */
    fun at(phase: Float): Float {
        val p = (phase - floor(phase)) * SIZE
        val i = p.toInt()
        return table[i] + (table[i + 1] - table[i]) * (p - i)
    }
}

/** Reparto de potencia constante: un sonido centrado no suena mas flojo que uno a un lado. */
internal fun panLeft(pan: Float) = Sine.at(0.25f + pan * 0.25f)

internal fun panRight(pan: Float) = Sine.at(pan * 0.25f)

internal class LowPass(hz: Float) {
    private var a = 0f
    private var y = 0f

    init {
        cutoff(hz)
    }

    fun cutoff(hz: Float) {
        a = 1f - exp(-TWO_PI * hz / SAMPLE_RATE)
    }

    fun process(x: Float): Float {
        y += a * (x - y)
        return y
    }
}

internal class HighPass(hz: Float) {
    private val low = LowPass(hz)

    fun process(x: Float): Float = x - low.process(x)
}

/** Paso banda de variable de estado (Simper), con ganancia 1 en el centro. Se puede mover sin chasquidos. */
internal class BandPass(hz: Float, q: Float) {
    private var k = 0f
    private var a1 = 0f
    private var a2 = 0f
    private var a3 = 0f
    private var ic1 = 0f
    private var ic2 = 0f

    init {
        tune(hz, q)
    }

    fun tune(hz: Float, q: Float) {
        val g = tan(PI * hz.coerceAtMost(SAMPLE_RATE * 0.45f) / SAMPLE_RATE).toFloat()
        k = 1f / q
        a1 = 1f / (1f + g * (g + k))
        a2 = g * a1
        a3 = g * a2
    }

    fun process(x: Float): Float {
        val v3 = x - ic2
        val v1 = a1 * ic1 + a2 * v3
        val v2 = ic2 + a2 * ic1 + a3 * v3
        ic1 = 2f * v1 - ic1
        ic2 = 2f * v2 - ic2
        return k * v1
    }
}

/** Ruido rosa (filtro de Paul Kellet): el ruido blanco con los agudos mas suaves. */
internal class Pink(private val rng: Rng) {
    private var b0 = 0f
    private var b1 = 0f
    private var b2 = 0f
    private var b3 = 0f
    private var b4 = 0f
    private var b5 = 0f
    private var b6 = 0f

    fun next(): Float {
        val w = rng.white()
        b0 = 0.99886f * b0 + w * 0.0555179f
        b1 = 0.99332f * b1 + w * 0.0750759f
        b2 = 0.96900f * b2 + w * 0.1538520f
        b3 = 0.86650f * b3 + w * 0.3104856f
        b4 = 0.55000f * b4 + w * 0.5329522f
        b5 = -0.7616f * b5 - w * 0.0168980f
        val out = b0 + b1 + b2 + b3 + b4 + b5 + b6 + w * 0.5362f
        b6 = w * 0.115926f
        return out * 0.11f
    }
}

/** Ruido marron: el blanco acumulado, casi todo graves. */
internal class Brown(private val rng: Rng) {
    private var y = 0f

    fun next(): Float {
        y = (y + 0.02f * rng.white()) / 1.02f
        return y * 3.5f
    }
}

/** Valor que pasea al azar entre [low] y [high], cambiando de rumbo cada pocos segundos. */
internal class Drift(
    private val rng: Rng,
    private val low: Float,
    private val high: Float,
    private val minSeconds: Float,
    private val maxSeconds: Float
) {
    private var from = rng.range(low, high)
    private var to = rng.range(low, high)
    private var pos = 0f
    private var step = nextStep()

    private fun nextStep() = 1f / (rng.range(minSeconds, maxSeconds) * SAMPLE_RATE)

    fun next(): Float {
        pos += step
        if (pos >= 1f) {
            pos = 0f
            from = to
            to = rng.range(low, high)
            step = nextStep()
        }
        return from + (to - from) * pos * pos * (3f - 2f * pos)
    }
}

/**
 * Sonidos cortos que se solapan: gotas, chasquidos, tintineos o campanas. Cada uno empieza de golpe
 * y se apaga solo. Tras [next], la muestra de todos juntos queda en [left] y [right].
 */
internal class Grains(private val capacity: Int) {
    private val amp = FloatArray(capacity)
    private val decay = FloatArray(capacity)
    private val gainLeft = FloatArray(capacity)
    private val gainRight = FloatArray(capacity)
    private val phase = FloatArray(capacity)
    private val step = FloatArray(capacity)
    private val glide = FloatArray(capacity)
    private var count = 0

    var left = 0f
        private set
    var right = 0f
        private set

    /**
     * [seconds]: tiempo en que baja a un 37 %. [hz] 0 es un chasquido de ruido; si no, un tono que
     * multiplica su frecuencia por [glide] en ese mismo tiempo.
     */
    fun add(amplitude: Float, seconds: Float, pan: Float, hz: Float = 0f, glide: Float = 1f) {
        if (count == capacity) return
        val i = count++
        val samples = seconds * SAMPLE_RATE
        amp[i] = amplitude
        decay[i] = exp(-1f / samples)
        gainLeft[i] = panLeft(pan)
        gainRight[i] = panRight(pan)
        phase[i] = 0f
        step[i] = hz / SAMPLE_RATE
        this.glide[i] = exp(ln(glide) / samples)
    }

    fun next(rng: Rng) {
        var l = 0f
        var r = 0f
        var i = 0
        while (i < count) {
            val s = if (step[i] == 0f) rng.white() * amp[i] else Sine.at(phase[i]) * amp[i]
            l += s * gainLeft[i]
            r += s * gainRight[i]
            phase[i] += step[i]
            if (phase[i] >= 1f) phase[i] -= 1f
            step[i] *= glide[i]
            amp[i] *= decay[i]
            if (amp[i] < 0.0005f) {
                // Se ha apagado: el ultimo pasa a su sitio.
                count--
                amp[i] = amp[count]
                decay[i] = decay[count]
                gainLeft[i] = gainLeft[count]
                gainRight[i] = gainRight[count]
                phase[i] = phase[count]
                step[i] = step[count]
                glide[i] = glide[count]
            } else {
                i++
            }
        }
        left = l
        right = r
    }
}
