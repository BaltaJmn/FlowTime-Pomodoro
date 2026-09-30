package com.baltajmn.flowtime.core.design.sound

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.core.design.R
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Mantiene vivos los sonidos con la app en segundo plano y la pantalla apagada. Se para solo cuando
 * no suena nada ni hay nada en pausa. Cede el sonido a las llamadas y a otras apps, y para si se
 * desconectan los auriculares.
 */
class AmbientService : Service() {

    private val ambience: Ambience by inject()
    private val scope = MainScope()
    private val audio by lazy { getSystemService(AudioManager::class.java) }
    private var resumeWithFocus = false

    private val focus by lazy {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(AmbientMixer.ATTRIBUTES)
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    // Una llamada o un aviso largo: vuelve al terminar. Con avisos cortos el sistema
                    // baja el volumen solo.
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                        resumeWithFocus = ambience.state.value.playing.isNotEmpty()
                        ambience.pause()
                    }

                    // Otra app de música: no vuelve sola.
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        resumeWithFocus = false
                        ambience.pause()
                    }

                    AudioManager.AUDIOFOCUS_GAIN -> if (resumeWithFocus) {
                        resumeWithFocus = false
                        ambience.resume()
                    }
                }
            }
            .build()
    }

    private val headphones = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = ambience.pause()
    }

    override fun onCreate() {
        super.onCreate()
        ContextCompat.registerReceiver(
            this,
            headphones,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        // El temporizador de apagado: con la app cerrada, lo que sigue vivo es este servicio.
        scope.launch {
            ambience.sleep.collectLatest { timer ->
                if (timer !is SleepTimer.At) return@collectLatest
                delay(timer.millis - System.currentTimeMillis())
                ambience.fadeOutAndStop()
            }
        }
        scope.launch {
            ambience.state.collect { state ->
                if (state.playing.isEmpty() && state.paused.isEmpty()) return@collect stopSelf()
                if (state.playing.isNotEmpty() &&
                    audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_FAILED
                ) {
                    return@collect ambience.pause()
                }
                post(notification(state))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // En cada arranque, no solo en el primero: cada startForegroundService lo exige.
        ServiceCompat.startForeground(
            this,
            ID,
            notification(ambience.state.value),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        when (intent?.action) {
            ACTION_PAUSE -> ambience.pause()
            ACTION_RESUME -> ambience.resume()
            ACTION_STOP -> ambience.stop()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        audio.abandonAudioFocusRequest(focus)
        unregisterReceiver(headphones)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    private fun notification(state: SoundState): Notification {
        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.channel_sounds))
                .setShowBadge(false)
                .build()
        )
        val paused = state.playing.isEmpty()
        val sounds = (if (paused) state.paused else state.playing).joinToString { getString(it.label) }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_music)
            .setContentTitle(getString(if (paused) R.string.time_title_paused else R.string.channel_sounds))
            .setContentText(sounds)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    packageManager.getLaunchIntentForPackage(packageName),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .addAction(
                0,
                getString(if (paused) R.string.action_resume else R.string.action_pause),
                command(if (paused) ACTION_RESUME else ACTION_PAUSE)
            )
            .addAction(0, getString(R.string.action_stop), command(ACTION_STOP))
            .setOngoing(true)
            .setSilent(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun command(action: String) = PendingIntent.getService(
        this,
        action.hashCode(),
        Intent(this, AmbientService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE
    )

    private fun post(notification: Notification) {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (permission == PackageManager.PERMISSION_GRANTED) NotificationManagerCompat.from(this).notify(ID, notification)
    }

    private companion object {
        const val ID = 3
        const val CHANNEL = "sounds"
        const val ACTION_PAUSE = "pause"
        const val ACTION_RESUME = "resume"
        const val ACTION_STOP = "stop"
    }
}
