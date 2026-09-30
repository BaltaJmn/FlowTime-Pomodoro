package com.baltajmn.flowtime.session

import android.Manifest
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.MainActivity
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusState
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.PhaseChange
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.data.timer.TimerMode

/**
 * Las notificaciones de la sesión: la que está mientras dura, y el aviso al terminar cada fase.
 * El reloj lo dibuja el sistema, así que no hay que actualizarla cada segundo ni hace falta un
 * servicio: sigue contando aunque se cierre la app.
 */
class SessionNotification(
    private val context: Context,
    private val engine: FocusEngine,
    private val tags: TagRepository
) {
    private val manager = NotificationManagerCompat.from(context)

    fun update(state: FocusState = engine.state.value) {
        if (!state.isActive) return manager.cancel(ID)

        // Importancia normal pero sin sonido ni vibración: las de importancia baja van plegadas al
        // final, sin botones a la vista, y en muchos móviles no salen en la pantalla de bloqueo.
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.channel_session))
                .setSound(null, null)
                .setVibrationEnabled(false)
                .setShowBadge(false)
                .build()
        )
        val snapshot = engine.snapshot(state)
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(state.mode.icon)
            .setContentTitle(context.getString(state.title))
            .setSubText(subText(state))
            .setContentIntent(openTimer(state.mode))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (state.running) {
            val now = System.currentTimeMillis()
            builder
                .setUsesChronometer(true)
                .setChronometerCountDown(state.countsDown)
                .setWhen(
                    if (state.countsDown) now + snapshot.remainingMillis else now - snapshot.elapsedMillis
                )
                .setContentText(state.taskTitle)
        } else {
            val time = snapshot.displaySeconds.formatSecondsToTime()
            builder.setShowWhen(false)
                .setContentText(listOfNotNull(time, state.taskTitle).joinToString(" · "))
        }
        state.actions.forEach { action ->
            builder.addAction(0, context.getString(action.label), perform(action))
        }
        post(ID, builder.build())
    }

    /**
     * El aviso de fin de fase, con sonido y vibración. Un canal para cuando acaba el trabajo y otro
     * para cuando acaba el descanso: cada uno con su sonido, que se puede cambiar en los ajustes
     * del sistema. Como un canal no deja cambiar su sonido una vez creado, si algún día se cambian
     * los ficheros habrá que crear canales con otro id.
     */
    fun alert(change: PhaseChange) {
        val workEnded = change.from == Phase.WORK
        val channel = if (workEnded) CHANNEL_WORK_END else CHANNEL_BREAK_END
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(channel, NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(
                    context.getString(
                        if (workEnded) R.string.channel_work_end else R.string.channel_break_end
                    )
                )
                .setSound(
                    Uri.parse(
                        "android.resource://${context.packageName}/raw/${if (workEnded) "confirmation" else "start"}"
                    ),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setVibrationEnabled(true)
                .build()
        )
        val title = when (change.to) {
            Phase.BREAK -> R.string.alert_break
            Phase.WORK -> R.string.alert_focus
            Phase.IDLE -> R.string.alert_session_over
        }
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(change.mode.icon)
            .setContentTitle(context.getString(title))
            .setSubText(context.getString(change.mode.label))
            .setContentIntent(openTimer(change.mode))
            .setAutoCancel(true)
            // Un temporizador que termina es una alarma: si No molestar deja pasar las alarmas, suena.
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        // Al acabar un bloque con tarea, completarla sin abrir la app (#40).
        val state = engine.state.value
        val taskId = state.taskId
        if (workEnded && taskId != null) {
            val question = context.getString(R.string.task_done_question, state.taskTitle.orEmpty())
            val done = context.getString(R.string.task_done_action)
            builder.setContentText(question).addAction(0, done, completeTask(taskId))
        }
        post(ALERT_ID, builder.build())
    }

    /** Al volver a la app, el aviso ya ha hecho su trabajo. */
    fun dismissAlert() = manager.cancel(ALERT_ID)

    // Sin permiso la app sigue funcionando con la pantalla abierta; Ajustes avisa de ello.
    private fun post(id: Int, notification: Notification) {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (permission == PackageManager.PERMISSION_GRANTED) manager.notify(id, notification)
    }

    /** "Pomodoro · Estudio", o solo el modo si no hay etiqueta. */
    private fun subText(state: FocusState): String {
        val tag = state.tagId?.let { id -> tags.all.value.firstOrNull { it.id == id } }
        return listOfNotNull(context.getString(state.mode.label), tag?.name).joinToString(" · ")
    }

    private fun openTimer(mode: TimerMode) = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(EXTRA_OPEN_TIMER, mode.name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun completeTask(id: Long) = PendingIntent.getBroadcast(
        context,
        REQUEST_COMPLETE_TASK,
        Intent(context, SessionReceiver::class.java)
            .setAction(ACTION_COMPLETE_TASK)
            .putExtra(EXTRA_TASK_ID, id),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    private fun perform(action: TimerAction) = PendingIntent.getBroadcast(
        context,
        action.ordinal,
        Intent(context, SessionReceiver::class.java).setAction(action.name),
        PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val ID = 1
        private const val ALERT_ID = 2
        private const val CHANNEL = "session"
        private const val CHANNEL_WORK_END = "work_end"
        private const val CHANNEL_BREAK_END = "break_end"
        private const val EXTRA_OPEN_TIMER = "open_timer"

        // Otro que los de las acciones del temporizador, que usan su ordinal.
        private const val REQUEST_COMPLETE_TASK = 100
        const val ACTION_COMPLETE_TASK = "COMPLETE_TASK"
        const val EXTRA_TASK_ID = "task_id"

        /** El modo cuya pantalla hay que abrir si se ha llegado pulsando la notificación. Solo una vez. */
        fun timerToOpen(intent: Intent): TimerMode? {
            val name = intent.getStringExtra(EXTRA_OPEN_TIMER) ?: return null
            intent.removeExtra(EXTRA_OPEN_TIMER)
            return TimerMode.entries.firstOrNull { it.name == name }
        }
    }
}

/** Trabajando, descansando o en pausa. */
internal val FocusState.title
    get() = when {
        isPaused -> R.string.time_title_paused
        phase == Phase.BREAK -> R.string.time_title_resting
        else -> R.string.time_title_working
    }

/** El nombre y el icono monocromo de cada modo, para la notificación, el widget y el botón rápido. */
internal val TimerMode.label
    get() = when (this) {
        TimerMode.POMODORO -> R.string.mode_pomodoro
        TimerMode.FLOW_TIME -> R.string.mode_flow_time
        TimerMode.PERCENTAGE -> R.string.mode_percentage
    }

internal val TimerMode.icon
    get() = when (this) {
        TimerMode.POMODORO -> R.drawable.ic_pomodoro
        TimerMode.FLOW_TIME -> R.drawable.ic_flowtime
        TimerMode.PERCENTAGE -> R.drawable.ic_percentage
    }
