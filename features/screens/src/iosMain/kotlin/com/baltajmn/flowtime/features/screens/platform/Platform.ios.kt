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
import platform.darwin.dispatch_time
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_after
import platform.darwin.NSObject
import platform.darwin.DISPATCH_TIME_NOW
import platform.UniformTypeIdentifiers.UTType
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIAlertController
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSFileManager
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.alert_app_store
import com.baltajmn.flowtime.core.design.resources.backup_automatic_ios
import com.baltajmn.flowtime.core.design.resources.notifications_off_ios
import com.baltajmn.flowtime.core.design.resources.notifications_text_ios
import com.baltajmn.flowtime.core.design.resources.notifications_title_ios
import com.baltajmn.flowtime.core.design.resources.pro_restore_nothing_ios
import com.baltajmn.flowtime.core.design.resources.pro_settings_text_ios
import com.baltajmn.flowtime.core.design.resources.restore_nothing_ios
import org.jetbrains.compose.resources.StringResource
import androidx.compose.runtime.rememberUpdatedState

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

actual val hasFocusMode: Boolean = false

actual val automaticBackupText: StringResource = Res.string.backup_automatic_ios

actual val storeUnavailableText: StringResource = Res.string.alert_app_store

actual val restoreNothingText: StringResource = Res.string.restore_nothing_ios

actual val proRestoreNothingText: StringResource = Res.string.pro_restore_nothing_ios

actual val proSettingsText: StringResource = Res.string.pro_settings_text_ios

actual val notificationsTitle: StringResource = Res.string.notifications_title_ios

actual val notificationsText: StringResource = Res.string.notifications_text_ios

actual val notificationsOffText: StringResource = Res.string.notifications_off_ios

/** Como un Toast: un aviso que se va solo a los dos segundos. */
@Composable
actual fun rememberShowMessage(): (String) -> Unit {
    val controller = LocalUIViewController.current
    return remember(controller) {
        { text ->
            val alert = UIAlertController.alertControllerWithTitle(null, text, UIAlertControllerStyleAlert)
            controller.presentViewController(alert, animated = true) {
                dispatch_after(dispatch_time(DISPATCH_TIME_NOW, MESSAGE_NANOS), dispatch_get_main_queue()) {
                    alert.dismissViewControllerAnimated(true, null)
                }
            }
        }
    }
}

private const val MESSAGE_NANOS = 2_000_000_000L

/**
 * El selector del iPhone guarda un fichero que ya existe: se le da uno vacío con ese nombre, lo mueve
 * a donde se elija, y después se llena.
 */
@Composable
actual fun rememberCreateFile(mime: String, onPicked: (PickedFile) -> Unit): (suggestedName: String) -> Unit {
    val controller = LocalUIViewController.current
    val picker = rememberPickerDelegate(onPicked)
    return remember(controller, picker) {
        { name ->
            val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + name)
            NSFileManager.defaultManager.createFileAtPath(url.path.orEmpty(), null, null)
            val document = UIDocumentPickerViewController(forExportingURLs = listOf(url), asCopy = false)
            document.delegate = picker
            controller.presentViewController(document, animated = true, completion = null)
        }
    }
}

@Composable
actual fun rememberOpenFile(mimeTypes: List<String>, onPicked: (PickedFile) -> Unit): () -> Unit {
    val controller = LocalUIViewController.current
    val picker = rememberPickerDelegate(onPicked)
    return remember(controller, picker, mimeTypes) {
        {
            val types = mimeTypes.mapNotNull { UTType.typeWithMIMEType(it) }
            val document = UIDocumentPickerViewController(forOpeningContentTypes = types, asCopy = true)
            document.delegate = picker
            controller.presentViewController(document, animated = true, completion = null)
        }
    }
}

// El selector no retiene a su delegado: lo guarda la pantalla.
@Composable
private fun rememberPickerDelegate(onPicked: (PickedFile) -> Unit): PickerDelegate {
    val picked by rememberUpdatedState(onPicked)
    return remember { PickerDelegate { picked(PickedFile(it)) } }
}

private class PickerDelegate(private val onPicked: (NSURL) -> Unit) :
    NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(controller: UIDocumentPickerViewController, didPickDocumentsAtURLs: List<*>) {
        (didPickDocumentsAtURLs.firstOrNull() as? NSURL)?.let(onPicked)
    }
}

/** Si el formato de hora del idioma lleva AM/PM, va en 12 horas. */
@Composable
actual fun is24HourClock(): Boolean =
    NSDateFormatter.dateFormatFromTemplate("j", 0u, NSLocale.currentLocale)?.contains('a') != true
