package com.baltajmn.flowtime.features.screens.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.uikit.LocalUIViewController
import com.baltajmn.flowtime.data.backup.PickedFile
import com.baltajmn.flowtime.data.pro.PurchaseHost
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.currentLocale
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIWindowScene
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNAuthorizationStatus
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.MutableState

/** El permiso de notificaciones. Se vuelve a leer cada vez que se vuelve a la app: se cambia en Ajustes. */
@Composable
private fun rememberNotificationStatus(): MutableState<UNAuthorizationStatus> {
    val status = remember { mutableStateOf(UNAuthorizationStatusNotDetermined) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    LaunchedEffect(lifecycle) {
        if (lifecycle != Lifecycle.State.RESUMED) return@LaunchedEffect
        UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
            settings?.let { status.value = it.authorizationStatus }
        }
    }
    return status
}

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val status = rememberNotificationStatus()
    return remember {
        NotificationPermission(
            granted = { status.value == UNAuthorizationStatusAuthorized || status.value == UNAuthorizationStatusProvisional },
            ask = {
                val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { granted, _ ->
                    status.value = if (granted) UNAuthorizationStatusAuthorized else UNAuthorizationStatusDenied
                }
            }
        )
    }
}

@Composable
actual fun rememberSystemSettings(): SystemSettings {
    val status = rememberNotificationStatus()
    // Sin preguntar todavía no hay aviso: lo pide Concentración al empezar la primera sesión, y hasta
    // entonces Ajustes del iPhone no tiene el interruptor.
    return remember { IosSystemSettings(notificationsAllowed = { status.value != UNAuthorizationStatusDenied }) }
}

/** En el iPhone no hay alarmas exactas que permitir ni No molestar que encender desde una app. */
private class IosSystemSettings(private val notificationsAllowed: () -> Boolean) : SystemSettings {
    override fun notificationsAllowed() = notificationsAllowed.invoke()

    override fun openNotificationSettings() {
        NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let {
            UIApplication.sharedApplication.openURL(it, emptyMap<Any?, Any>(), null)
        }
    }

    override fun exactAlarmsAllowed() = true

    override fun openExactAlarmSettings() = Unit

    override val hasFocusMode = false

    override fun openFocusModeAccess() = Unit

    /** La valoración del propio sistema: no necesita el id de la app en la App Store. */
    override fun openStoreListing() {
        val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene
        scene?.let { SKStoreReviewController.requestReviewInScene(it) }
    }
}

@Composable
actual fun KeepScreenOn(active: Boolean) {
    DisposableEffect(active) {
        UIApplication.sharedApplication.idleTimerDisabled = active
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

@Composable
actual fun rememberPurchaseHost(): PurchaseHost? = LocalUIViewController.current

actual val canBlur: Boolean = true

actual val hasWallpaperColors: Boolean = false

actual val hasAppIcons: Boolean = false

actual val hasFiles: Boolean = false

// ponytail: sin aviso en el iPhone. Los mensajes de ahora son de los ficheros (que aún no hay) y de
// restaurar compras; cuando haga falta, un snackbar.
@Composable
actual fun rememberShowMessage(): (String) -> Unit = remember { { _ -> } }

@Composable
actual fun rememberCreateFile(mime: String, onPicked: (PickedFile) -> Unit): (suggestedName: String) -> Unit =
    remember { { _ -> } }

@Composable
actual fun rememberOpenFile(mimeTypes: List<String>, onPicked: (PickedFile) -> Unit): () -> Unit = remember { {} }

/** Si el formato de hora del idioma lleva AM/PM, va en 12 horas. */
@Composable
actual fun is24HourClock(): Boolean =
    NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale.currentLocale)?.contains('a') != true
