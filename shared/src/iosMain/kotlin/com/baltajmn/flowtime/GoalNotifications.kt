package com.baltajmn.flowtime

import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.goal_reached
import com.baltajmn.flowtime.core.design.resources.goal_reached_text
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.GOAL_CELEBRATED_ON
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * El objetivo de hoy cumplido con la app cerrada, como GoalNotification en Android. Una app suspendida
 * no guarda la sesión al acabar la fase, así que se calcula de antemano en qué fin de trabajo se llega
 * al objetivo si nadie toca la sesión, y el aviso se programa para entonces. Se rehace con cada cambio
 * de la sesión o del progreso de hoy, como PhaseNotifications. Con la app delante no se enseña
 * (NotificationDelegate): lo celebra la app.
 */
internal class GoalNotifications(
    private val engine: FocusEngine,
    private val goals: GoalRepository,
    private val dataProvider: DataProvider
) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    suspend fun schedule() {
        val today = goals.today.first()
        val title = getString(Res.string.goal_reached)
        val text = getString(Res.string.goal_reached_text, today.goalMinutes.toLong().formatMinutesStudying())
        // Sin suspender desde aquí, como en PhaseNotifications.
        center.removePendingNotificationRequestsWithIdentifiers(listOf(ID))
        if (today.met || dataProvider.getString(GOAL_CELEBRATED_ON) == today.day.toString()) return
        val inMillis = reachedIn(today) ?: return
        // Pasada la medianoche cuenta para otro día, con su propio objetivo.
        val at = (Clock.System.now() + inMillis.milliseconds).toLocalDateTime(TimeZone.currentSystemDefault())
        if (at.date != today.day) return
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(text)
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            (inMillis / 1000.0).coerceAtLeast(1.0),
            repeats = false
        )
        center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(ID, content, trigger), null)
    }

    /** Lo que falta para el fin de trabajo que cumple el objetivo, o null si la sesión no llega sola. */
    private fun reachedIn(today: DayProgress): Long? {
        val state = engine.state.value
        var seconds = today.seconds
        engine.upcomingChanges(MAX_CHANGES, state).forEachIndexed { index, (inMillis, change) ->
            if (change.from != Phase.WORK) return@forEachIndexed
            // La primera es la fase de ahora; las demás, un bloque de trabajo entero del modo.
            seconds += (if (index == 0) state.durationMillis else engine.workMillis(change.mode)) / 1000
            if (seconds >= today.goalMinutes * 60L) return inMillis
        }
        return null
    }

    companion object {
        const val ID = "goal_reached"

        // Los mismos que avisa PhaseNotifications.
        private const val MAX_CHANGES = 8
    }
}
