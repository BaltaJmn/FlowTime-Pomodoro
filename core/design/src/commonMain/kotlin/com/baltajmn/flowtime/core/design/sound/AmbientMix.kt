package com.baltajmn.flowtime.core.design.sound

import kotlin.concurrent.Volatile
import kotlin.math.abs

/**
 * Mezcla los sonidos ambientales activos en una sola salida de audio. La salida de cada plataforma
 * ([AmbientMixer]) solo abre el audio mientras suena algo y va pidiendo bloques de [FRAMES] muestras.
 */
expect class AmbientMixer() {
    /** Volumen de 0 a 1 de un sonido; 0 lo para. */
    fun setVolume(type: PlayerType, volume: Float)

    /** El volumen de toda la mezcla, para el fundido del temporizador de apagado. */
    var master: Float
}

/** Lo que comparten las salidas de audio: el volumen de cada sonido y el cálculo de cada bloque. */
internal class AmbientMix {

    private val ambients = arrayOfNulls<Ambient>(PlayerType.entries.size)
    private val gains = FloatArray(ambients.size)
    private val left = FloatArray(FRAMES)
    private val right = FloatArray(FRAMES)
    private val scratchLeft = FloatArray(FRAMES)
    private val scratchRight = FloatArray(FRAMES)

    // Se sustituye entera en cada cambio: el hilo de audio siempre lee una copia completa.
    @Volatile
    private var targets = FloatArray(PlayerType.entries.size)

    @Volatile
    var master = 1f

    /** Si ya no queda ningún sonido por sonar: la salida puede cerrarse. */
    val silent: Boolean get() = targets.all { it == 0f }

    fun setVolume(type: PlayerType, volume: Float) {
        // Al cuadrado: el oido nota igual los pasos del principio y del final del deslizador.
        targets = targets.copyOf().also { it[type.ordinal] = volume * volume }
    }

    /** Escribe en [out] un bloque en estéreo intercalado. Devuelve si ha sonado algo. */
    fun render(out: FloatArray): Boolean {
        val target = targets
        left.fill(0f)
        right.fill(0f)
        var audible = false
        for (i in target.indices) {
            val to = target[i]
            var gain = gains[i]
            if (gain == 0f && to == 0f) continue
            audible = true
            val ambient = ambients[i] ?: ambientFor(PlayerType.entries[i]).also { ambients[i] = it }
            ambient.render(scratchLeft, scratchRight, FRAMES)
            for (n in 0 until FRAMES) {
                // Rampa corta hacia el volumen pedido: sin chasquidos al mover el deslizador.
                gain = if (gain < to) minOf(gain + RAMP, to) else maxOf(gain - RAMP, to)
                left[n] += scratchLeft[n] * gain
                right[n] += scratchRight[n] * gain
            }
            gains[i] = gain
        }
        val master = master
        for (n in 0 until FRAMES) {
            out[2 * n] = softClip(left[n] * master)
            out[2 * n + 1] = softClip(right[n] * master)
        }
        return audible
    }

    companion object {
        const val FRAMES = 1024
        const val BLOCKS_PER_SECOND = SAMPLE_RATE / FRAMES
        private const val RAMP = 1f / (0.05f * SAMPLE_RATE)

        /** Deja pasar la señal hasta 0,7 y por encima la dobla poco a poco, sin pasar nunca de 1. */
        private fun softClip(x: Float): Float {
            val a = abs(x)
            if (a <= 0.7f) return x
            val over = (a - 0.7f) / 0.3f
            val y = 0.7f + 0.3f * over / (1f + over)
            return if (x > 0f) y else -y
        }
    }
}
