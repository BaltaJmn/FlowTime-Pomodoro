package com.baltajmn.flowtime.core.design.sound

import com.baltajmn.flowtime.core.design.sound.AmbientMix.Companion.BLOCKS_PER_SECOND
import com.baltajmn.flowtime.core.design.sound.AmbientMix.Companion.FRAMES
import kotlin.concurrent.Volatile
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.get
import kotlinx.cinterop.set
import platform.AVFAudio.AVAudioEngine
import platform.AVFAudio.AVAudioEngineConfigurationChangeNotification
import platform.AVFAudio.AVAudioFormat
import platform.AVFAudio.AVAudioPCMBuffer
import platform.AVFAudio.AVAudioPlayerNode
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.setActive
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber
import platform.Foundation.NSOperationQueue
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

/**
 * En el iPhone, con AVAudioEngine: un reproductor con [QUEUED] bloques ya calculados siempre en cola,
 * para que una pausa del recolector de memoria no llegue a oírse. Los bloques se calculan de uno en
 * uno en su propia cola; abrir y cerrar el audio va por el hilo principal. Como en Android, tras un
 * segundo en silencio se cierra.
 */
@OptIn(ExperimentalForeignApi::class)
actual class AmbientMixer actual constructor() {

    private val mix = AmbientMix()
    private val format = AVAudioFormat(standardFormatWithSampleRate = SAMPLE_RATE.toDouble(), channels = 2u)
    private val render = dispatch_queue_create("ambient-sound", null)
    private val out = FloatArray(FRAMES * 2)
    private var silentBlocks = 0
    private var engine: AVAudioEngine? = null

    // El que suena ahora: los bloques de uno ya parado no piden más.
    @Volatile
    private var player: AVAudioPlayerNode? = null

    init {
        val center = NSNotificationCenter.defaultCenter
        // Una llamada o la alarma de otra app paran el audio; al terminar, vuelve a sonar.
        center.addObserverForName(AVAudioSessionInterruptionNotification, null, NSOperationQueue.mainQueue) { note ->
            val type = (note?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)?.unsignedIntegerValue
            if (type == AVAudioSessionInterruptionTypeEnded) restart()
        }
        // Cambiar de salida (quitar los auriculares, conectar unos por Bluetooth) para el motor.
        center.addObserverForName(AVAudioEngineConfigurationChangeNotification, null, NSOperationQueue.mainQueue) {
            restart()
        }
    }

    actual var master: Float
        get() = mix.master
        set(value) {
            mix.master = value
        }

    actual fun setVolume(type: PlayerType, volume: Float) {
        mix.setVolume(type, volume)
        if (engine == null && volume > 0f) start()
    }

    private fun start() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
        val engine = AVAudioEngine()
        val player = AVAudioPlayerNode()
        engine.attachNode(player)
        engine.connect(player, to = engine.mainMixerNode, format = format)
        if (!engine.startAndReturnError(null)) return
        this.engine = engine
        this.player = player
        dispatch_async(render) {
            silentBlocks = 0
            repeat(QUEUED) { schedule(player) }
            dispatch_async(dispatch_get_main_queue()) { player.play() }
        }
    }

    private fun stop() {
        // Antes de pararlo: al parar, los bloques en cola avisan de que han terminado.
        val player = player ?: return
        this.player = null
        player.stop()
        engine?.stop()
        engine = null
        // Si mientras tanto ha vuelto a sonar algo, sigue.
        if (!mix.silent) start()
    }

    /** Vuelve a empezar si estaba sonando: [stop] arranca otra vez si queda algo que suene. */
    private fun restart() {
        if (player != null) stop()
    }

    /** En la cola de cálculo: un bloque más, y al terminar de sonar, el siguiente. */
    private fun schedule(player: AVAudioPlayerNode) {
        val audible = mix.render(out)
        silentBlocks = if (audible) 0 else silentBlocks + 1
        if (silentBlocks > BLOCKS_PER_SECOND && mix.silent) {
            dispatch_async(dispatch_get_main_queue()) { if (this.player === player) stop() }
            return
        }
        val buffer = AVAudioPCMBuffer(pCMFormat = format, frameCapacity = FRAMES.toUInt())
        buffer.frameLength = FRAMES.toUInt()
        val channels = buffer.floatChannelData ?: return
        val left = channels[0] ?: return
        val right = channels[1] ?: return
        for (n in 0 until FRAMES) {
            left[n] = out[2 * n]
            right[n] = out[2 * n + 1]
        }
        player.scheduleBuffer(buffer) {
            if (this.player === player) dispatch_async(render) { schedule(player) }
        }
    }

    private companion object {
        const val QUEUED = 4
    }
}
