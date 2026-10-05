package com.baltajmn.flowtime

import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.alert_break
import com.baltajmn.flowtime.core.design.resources.alert_focus
import com.baltajmn.flowtime.core.design.resources.alert_session_over
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ALERT
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import org.jetbrains.compose.resources.getString
import platform.darwin.NSObject
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationPresentationOptionBanner
import platform.UserNotifications.UNNotificationPresentationOptionList
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationPresentationOptions
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol

/**
 * Los avisos de fin de fase en el iPhone. Una app suspendida no se despierta a su hora, así que se
 * programan de antemano los próximos finales de fase, y se vuelven a programar con cada cambio de la
 * sesión (empezar, pausar, saltar, parar) y al volver a la app: el permiso puede haber llegado después
 * de empezar, o haberse dado en Ajustes.
 */
internal class PhaseNotifications(private val engine: FocusEngine, private val dataProvider: DataProvider) {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    suspend fun schedule() {
        val titles = mapOf(
            Phase.BREAK to getString(Res.string.alert_break),
            Phase.WORK to getString(Res.string.alert_focus),
            Phase.IDLE to getString(Res.string.alert_session_over)
        )
        val modes = TimerMode.entries.associateWith { getString(it.label) }
        // Sin suspender desde aquí: con la sesión de ahora, aunque haya cambiado mientras se leían
        // los textos. Sin permiso, iOS no los guarda.
        center.removePendingNotificationRequestsWithIdentifiers(IDS)
        if (!dataProvider.getBoolean(SHOW_ALERT, true)) return
        engine.upcomingChanges(MAX).forEachIndexed { index, (inMillis, change) ->
            val content = UNMutableNotificationContent().apply {
                setTitle(titles.getValue(change.to))
                setBody(modes.getValue(change.mode))
                setSound(UNNotificationSound.defaultSound)
            }
            // El sistema no acepta menos de un segundo.
            val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
                (inMillis / 1000.0).coerceAtLeast(1.0),
                repeats = false
            )
            center.addNotificationRequest(UNNotificationRequest.requestWithIdentifier(IDS[index], content, trigger), null)
        }
    }

    private companion object {
        // ponytail: con la app sin abrir, avisa de 8 cambios (4 pomodoros con su descanso) y para.
        // Más, hasta 64 que deja iOS, si alguien lo echa en falta.
        const val MAX = 8
        val IDS = List(MAX) { "phase_end_$it" }
    }
}

/**
 * Con la app delante, el aviso se ve y suena igual que fuera, menos el del objetivo: ese lo celebra la
 * app. Tocarlo abre Concentración.
 */
internal class NotificationDelegate(private val onOpen: () -> Unit) :
    NSObject(),
    UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) = withCompletionHandler(
        if (willPresentNotification.request.identifier == GoalNotifications.ID) {
            0uL
        } else {
            UNNotificationPresentationOptionBanner or UNNotificationPresentationOptionList or UNNotificationPresentationOptionSound
        }
    )

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        onOpen()
        withCompletionHandler()
    }
}
