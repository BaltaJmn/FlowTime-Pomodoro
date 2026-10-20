package com.baltajmn.flowtime.core.design.sound

import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.channel_sounds
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess

/**
 * Los sonidos en "En reproducción" (pantalla de bloqueo y Centro de control), con reproducir y
 * pausa: lo que en Android hace la notificación de AmbientService. Así se ve que la app suena en
 * segundo plano, que es para lo que declara el modo de fondo "audio" (la revisión 2.5.4 de Apple).
 */
fun showInNowPlaying(ambience: Ambience) {
    val commands = MPRemoteCommandCenter.sharedCommandCenter()
    commands.playCommand.addTargetWithHandler {
        ambience.resume()
        MPRemoteCommandHandlerStatusSuccess
    }
    commands.pauseCommand.addTargetWithHandler {
        ambience.pause()
        MPRemoteCommandHandlerStatusSuccess
    }
    commands.togglePlayPauseCommand.addTargetWithHandler {
        ambience.toggle()
        MPRemoteCommandHandlerStatusSuccess
    }
    MainScope().launch {
        ambience.state.collect { state ->
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = state.shown.takeIf { it.isNotEmpty() }?.let { shown ->
                mapOf<Any?, Any?>(
                    MPMediaItemPropertyTitle to shown.map { getString(it.label) }.joinToString(),
                    MPMediaItemPropertyArtist to getString(Res.string.channel_sounds),
                    MPNowPlayingInfoPropertyPlaybackRate to if (state.playing.isEmpty()) 0.0 else 1.0
                )
            }
        }
    }
}
