package com.baltajmn.flowtime.core.design.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import com.baltajmn.flowtime.core.design.sound.AmbientMix.Companion.BLOCKS_PER_SECOND
import com.baltajmn.flowtime.core.design.sound.AmbientMix.Companion.FRAMES

/** En Android, con un AudioTrack. El hilo de audio solo existe mientras suena algo: tras un segundo en silencio se cierra. */
actual class AmbientMixer actual constructor() {

    private val mix = AmbientMix()
    private var thread: Thread? = null

    actual var master: Float
        get() = mix.master
        set(value) {
            mix.master = value
        }

    actual fun setVolume(type: PlayerType, volume: Float) = synchronized(this) {
        mix.setVolume(type, volume)
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
        val out = FloatArray(FRAMES * 2)
        var silentBlocks = 0

        track.play()
        while (true) {
            val audible = mix.render(out)
            track.write(out, 0, out.size, AudioTrack.WRITE_BLOCKING)

            silentBlocks = if (audible) 0 else silentBlocks + 1
            if (silentBlocks > BLOCKS_PER_SECOND) {
                val finished = synchronized(this) {
                    mix.silent.also { if (it) thread = null }
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
            .setAudioAttributes(AMBIENT_ATTRIBUTES)
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
}

/**
 * Como música: va con el volumen multimedia y el sistema la baja sola con los avisos cortos. Fuera de
 * [AmbientMixer]: Compose lee su estabilidad al cargar Ambience, y en los tests de JVM AudioAttributes no existe.
 */
internal val AMBIENT_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
    .setUsage(AudioAttributes.USAGE_MEDIA)
    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
    .build()
