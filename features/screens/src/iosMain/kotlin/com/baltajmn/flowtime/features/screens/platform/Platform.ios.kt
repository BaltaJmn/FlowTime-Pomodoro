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

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val center = remember { UNUserNotificationCenter.currentNotificationCenter() }
    var status by remember { mutableStateOf(UNAuthorizationStatusNotDetermined) }
    LaunchedEffect(Unit) {
        center.getNotificationSettingsWithCompletionHandler { settings -> settings?.let { status = it.authorizationStatus } }
    }
    return remember {
        NotificationPermission(
            granted = { status == UNAuthorizationStatusAuthorized || status == UNAuthorizationStatusProvisional },
            ask = {
                val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                center.requestAuthorizationWithOptions(options) { granted, _ ->
                    status = if (granted) UNAuthorizationStatusAuthorized else UNAuthorizationStatusDenied
                }
            }
        )
    }
}

@Composable
actual fun rememberSystemSettings(): SystemSettings = remember { IosSystemSettings }

/** En el iPhone no hay alarmas exactas que permitir ni No molestar que encender desde una app. */
private object IosSystemSettings : SystemSettings {
    // ponytail: no avisa si se han quitado los avisos en Ajustes; leerlo es asíncrono. Cuando el
    // iPhone avise de las fases, mirar el estado como rememberNotificationPermission.
    override fun notificationsAllowed() = true

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
