package com.baltajmn.flowtime.goal

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.FlowTimeActivity
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.goal.Celebration

/** La celebración del objetivo como notificación, para cuando la app no está a la vista (ver [GoalWatcher]). */
class GoalNotification(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** False sin permiso para notificar. */
    suspend fun notify(celebration: Celebration): Boolean {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        )
        if (permission != PackageManager.PERMISSION_GRANTED) return false
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.goal_title))
                .build()
        )
        val open = PendingIntent.getActivity(
            context,
            REQUEST_OPEN,
            Intent(context, FlowTimeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        manager.notify(
            ID,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_confetti)
                .setContentTitle(context.getString(R.string.goal_reached))
                .setContentText(celebration.text())
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
        return true
    }

    private companion object {
        const val CHANNEL = "daily_goal"

        // Los ids de notificación de la app: 1 la sesión, 2 el aviso de fin de fase, 3 los sonidos
        // (AmbientService), 4 el objetivo y 5 el recordatorio. Con el mismo id, una pisa a la otra.
        const val ID = 4

        // Otro que el de abrir el temporizador (0): con el mismo, FLAG_UPDATE_CURRENT le quitaría
        // al de la sesión el modo que tiene que abrir.
        const val REQUEST_OPEN = 1
    }
}
