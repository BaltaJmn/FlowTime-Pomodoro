package com.baltajmn.flowtime.core.design.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlin.math.abs

/**
 * Mezcla los sonidos ambientales activos en una sola salida de audio. El hilo de audio solo existe
 * mientras suena algo: tras un segundo en silencio se cierra.
 */
class AmbientMixer {

    private val ambients = arrayOfNulls<Ambient>(PlayerType.entries.size)

    // Se sustituye entera en cada cambio: el hilo de audio siempre lee una copia completa.
    @Volatile
    private var targets = FloatArray(PlayerType.entries.size)

    /** El volumen de toda la mezcla, para el fundido del temporizador de apagado. */
    @Volatile
    var master = 1f
    private var thread: Thread? = null

    /** Volumen de 0 a 1 de un sonido; 0 lo para. */
    fun setVolume(type: PlayerType, volume: Float) = synchronized(this) {
        // Al cuadrado: el oido nota igual los pasos del principio y del final del deslizador.
        targets = targets.copyOf().also { it[type.ordinal] = volume * volume }
        if (thread == null && volume > 0f) {
            thread = Thread(::play, "ambient-sound").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }
        }
    }

    private fun play() {
        val track = try {
            createTrack()
        } catch (e: Exception) {
            Log.e("AmbientMixer", "No se puede abrir la salida de audio", e)
            synchronized(this) { thread = null }
            return
        }
        val gains = FloatArray(ambients.size)
        val left = FloatArray(FRAMES)
        val right = FloatArray(FRAMES)
        val scratchLeft = FloatArray(FRAMES)
        val scratchRight = FloatArray(FRAMES)
        val out = FloatArray(FRAMES * 2)
        var silentBlocks = 0

        track.play()
        while (true) {
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
            track.write(out, 0, out.size, AudioTrack.WRITE_BLOCKING)

            silentBlocks = if (audible) 0 else silentBlocks + 1
            if (silentBlocks > BLOCKS_PER_SECOND) {
                val finished = synchronized(this) {
                    targets.all { it == 0f }.also { if (it) thread = null }
                }
                if (finished) break
                silentBlocks = 0
            }
        }
        track.stop()
        track.release()
    }

    private fun createTrack(): AudioTrack {
        val minBytes = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        return AudioTrack.Builder()
            .setAudioAttributes(ATTRIBUTES)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build()
            )
            .setBufferSizeInBytes(maxOf(minBytes, FRAMES * 2 * 4 * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    companion object {
        /** Como música: va con el volumen multimedia y el sistema la baja sola con los avisos cortos. */
        val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        private const val FRAMES = 1024
        private const val BLOCKS_PER_SECOND = SAMPLE_RATE / FRAMES
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
