package com.baltajmn.flowtime.goal

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.baltajmn.flowtime.MainActivity
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.GOAL_CELEBRATED_ON
import com.baltajmn.flowtime.data.goal.GoalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Haber cumplido el objetivo de hoy, con la racha que deja. */
data class Celebration(val goalMinutes: Int, val streak: Int) {
    /** 7, 30 o 100 días seguidos. */
    val milestone get() = streak in MILESTONES

    fun text(resources: Resources): String {
        val days = resources.getQuantityString(R.plurals.streak_days, streak, streak)
        return when {
            milestone -> resources.getString(R.string.streak_milestone, days)
            streak > 1 -> days
            else -> resources.getString(
                R.string.goal_reached_text,
                goalMinutes.toLong().formatMinutesStudying()
            )
        }
    }

    private companion object {
        val MILESTONES = setOf(7, 30, 100)
    }
}

/**
 * Celebra el objetivo diario una sola vez al día, al cumplirlo: con la app a la vista, dentro de la
 * app; si no, con una notificación.
 */
class GoalWatcher(
    private val context: Context,
    private val goals: GoalRepository,
    private val dataProvider: DataProvider
) {
    private val manager = NotificationManagerCompat.from(context)

    private val _celebration = MutableStateFlow<Celebration?>(null)

    /** La que la app tiene que enseñar, hasta que se cierre. */
    val celebration: StateFlow<Celebration?> = _celebration.asStateFlow()

    fun watch(scope: CoroutineScope) {
        scope.launch {
            goals.today.filter { it.met }.collect { progress ->
                val day = progress.day.toString()
                if (dataProvider.getString(GOAL_CELEBRATED_ON) == day) return@collect
                dataProvider.setString(GOAL_CELEBRATED_ON, day)
                val celebration = Celebration(progress.goalMinutes, goals.streak.first().current)
                val visible = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
                    Lifecycle.State.STARTED
                )
                // Sin permiso para notificar, se ve al volver a la app.
                if (visible || !notify(celebration)) _celebration.value = celebration
            }
        }
    }

    fun onShown() {
        _celebration.value = null
    }

    private fun notify(celebration: Celebration): Boolean {
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
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        manager.notify(
            ID,
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_confetti)
                .setContentTitle(context.getString(R.string.goal_reached))
                .setContentText(celebration.text(context.resources))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
        return true
    }

    private companion object {
        const val CHANNEL = "daily_goal"
        const val ID = 3

        // Otro que el de abrir el temporizador (0): con el mismo, FLAG_UPDATE_CURRENT le quitaría
        // al de la sesión el modo que tiene que abrir.
        const val REQUEST_OPEN = 1
    }
}
