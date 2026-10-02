package com.baltajmn.flowtime.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.FlowTimeActivity
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.reminder.Nudge
import com.baltajmn.flowtime.data.reminder.ReminderRepository
import com.baltajmn.flowtime.data.reminder.nudge
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.session.SessionReceiver
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/**
 * El recordatorio diario (#45), con un trabajo persistente de JobScheduler: no es exacto, pero unos
 * minutos de margen bastan, y el sistema lo conserva al reiniciar sin que la app necesite un receptor
 * de BOOT_COMPLETED. Lo despierta [ReminderJob] por [SessionReceiver], que también lo reprograma al
 * cambiar la hora o la zona horaria.
 */
class DailyReminder(
    private val context: Context,
    private val settings: ReminderRepository,
    private val goals: GoalRepository,
    private val engine: FocusEngine
) {
    private val jobs = context.getSystemService(JobScheduler::class.java)

    /** La siguiente, o ninguna si está apagado. */
    fun schedule() {
        // Hasta la 2.2.1 iba con AlarmManager, y una alarma pendiente sigue ahí tras actualizar:
        // sin cancelarla, ese día avisaría dos veces.
        PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            Intent(context, SessionReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )?.let(context.getSystemService(AlarmManager::class.java)::cancel)
        val now = Clock.System.now()
        val at = settings.reminder.value.next(now, TimeZone.currentSystemDefault())
            ?: return jobs.cancel(JOB_ID)
        jobs.schedule(
            JobInfo.Builder(JOB_ID, ComponentName(context, ReminderJob::class.java))
                .setMinimumLatency((at - now).inWholeMilliseconds.coerceAtLeast(0))
                .setPersisted(true)
                .build()
        )
    }

    /** Avisa si toca (sin el objetivo cumplido ni una sesión en marcha) y deja programada la siguiente. */
    suspend fun fire() {
        val nudge = nudge(goals.today.first(), goals.streak.first(), engine.state.value.isActive)
        nudge?.let(::notify)
        schedule()
    }

    private fun notify(nudge: Nudge) {
        val allowed = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (allowed != PackageManager.PERMISSION_GRANTED) return
        val manager = NotificationManagerCompat.from(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.channel_reminder))
                .build()
        )
        val text = when (nudge) {
            is Nudge.MinutesLeft -> context.getString(
                R.string.reminder_left,
                nudge.minutes.formatMinutesStudying()
            )
            is Nudge.KeepStreak -> context.getString(R.string.reminder_streak, nudge.days)
            Nudge.ShortSession -> context.getString(R.string.reminder_short)
        }
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, FlowTimeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE
        )
        val start = PendingIntent.getBroadcast(
            context,
            REQUEST_START,
            Intent(context, SessionReceiver::class.java).setAction(TimerAction.START.name),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_flowtime)
            .setContentTitle(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            // Se quita al empezar: el botón no abre la app.
            .setTimeoutAfter(TimeUnit.HOURS.toMillis(3))
            .addAction(0, context.getString(R.string.reminder_start), start)
            .build()
        manager.notify(ID, notification)
    }

    /** Al empezar una sesión, desde donde sea, el recordatorio sobra. */
    fun dismiss() = NotificationManagerCompat.from(context).cancel(ID)

    companion object {
        const val ACTION = "com.baltajmn.flowtime.REMINDER"
        private const val CHANNEL = "reminder"

        // Ver la lista de ids en GoalWatcher.
        private const val ID = 5

        // Lejos de los de WorkManager (Glance), que los reparte desde 0.
        private const val JOB_ID = 0x464C5754

        // Distintos de los de SessionNotification (el ordinal de cada acción y 100).
        private const val REQUEST_ALARM = 200
        private const val REQUEST_OPEN = 201
        private const val REQUEST_START = 202
    }
}

/** Despierta a [DailyReminder] a su hora. Lo demás, como con la alarma de antes, en SessionReceiver. */
class ReminderJob : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        sendBroadcast(Intent(this, SessionReceiver::class.java).setAction(DailyReminder.ACTION))
        return false
    }

    override fun onStopJob(params: JobParameters) = false
}
