package com.baltajmn.flowtime

import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.reminder_left
import com.baltajmn.flowtime.core.design.resources.reminder_short
import com.baltajmn.flowtime.core.design.resources.reminder_streak
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.reminder.Nudge
import com.baltajmn.flowtime.data.reminder.ReminderRepository
import com.baltajmn.flowtime.data.reminder.nudge
import com.baltajmn.flowtime.data.timer.FocusEngine
import kotlin.time.Clock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter

/**
 * El recordatorio diario (#45) en el iPhone. La app no se despierta a la hora para mirar cómo va el
 * día, así que se programan de antemano los próximos: el de hoy con lo que falta (o ninguno, con el
 * objetivo cumplido o una sesión en marcha) y los demás con el texto de un día sin empezar. Se rehace
 * con cada cambio del ajuste, del progreso de hoy o de la sesión, y al volver a la app.
 */
internal class ReminderNotifications(
    private val settings: ReminderRepository,
    private val goals: GoalRepository,
    private val engine: FocusEngine
) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    suspend fun schedule() {
        val today = goals.today.first()
        val todayText = nudge(today, goals.streak.first(), engine.state.value.isActive)?.let { text(it) }
        val laterText = getString(Res.string.reminder_short)
        // Sin suspender desde aquí, como en PhaseNotifications.
        center.removePendingNotificationRequestsWithIdentifiers(IDS)
        val zone = TimeZone.currentSystemDefault()
        var after = Clock.System.now()
        for (id in IDS) {
            val at = settings.reminder.value.next(after, zone) ?: break
            after = at
            val local = at.toLocalDateTime(zone)
            val text = if (local.date == today.day) todayText ?: continue else laterText
            val content = UNMutableNotificationContent().apply {
                setTitle(text)
                setSound(UNNotificationSound.defaultSound)
            }
            // A esa hora del reloj del móvil: si se cambia de zona horaria, sigue siendo a las 18:00.
            val components = NSDateComponents().apply {
                year = local.year.toLong()
                month = local.month.ordinal + 1L
                day = local.day.toLong()
                hour = local.hour.toLong()
                minute = local.minute.toLong()
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = false)
            center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(id, content, trigger), null)
        }
    }

    private suspend fun text(nudge: Nudge) = when (nudge) {
        is Nudge.MinutesLeft -> getString(Res.string.reminder_left, nudge.minutes.formatMinutesStudying())
        is Nudge.KeepStreak -> getString(Res.string.reminder_streak, nudge.days)
        Nudge.ShortSession -> getString(Res.string.reminder_short)
    }

    private companion object {
        // Una semana: al volver a la app se programa la siguiente.
        val IDS = List(7) { "reminder_$it" }
    }
}
