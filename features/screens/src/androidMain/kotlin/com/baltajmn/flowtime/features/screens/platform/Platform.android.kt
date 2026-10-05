package com.baltajmn.flowtime.features.screens.platform

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.data.backup.PickedFile
import com.baltajmn.flowtime.data.pro.PurchaseHost
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.alert_google_play
import com.baltajmn.flowtime.core.design.resources.backup_automatic
import com.baltajmn.flowtime.core.design.resources.notifications_off
import com.baltajmn.flowtime.core.design.resources.notifications_text
import com.baltajmn.flowtime.core.design.resources.notifications_title
import com.baltajmn.flowtime.core.design.resources.pro_restore_nothing
import com.baltajmn.flowtime.core.design.resources.pro_settings_text
import com.baltajmn.flowtime.core.design.resources.restore_nothing
import org.jetbrains.compose.resources.StringResource

@Composable
actual fun rememberNotificationPermission(): NotificationPermission {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(RequestPermission()) {}
    return remember(context, launcher) {
        NotificationPermission(
            granted = { notificationsAllowed(context) },
            ask = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
        )
    }
}

// Antes de Android 13 no hay permiso que pedir; ContextCompat lo resuelve con los ajustes de la app.
private fun notificationsAllowed(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED

@Composable
actual fun rememberSystemSettings(): SystemSettings {
    val context = LocalContext.current
    return remember(context) { AndroidSystemSettings(context) }
}

private class AndroidSystemSettings(private val context: Context) : SystemSettings {
    override fun notificationsAllowed() = NotificationManagerCompat.from(context).areNotificationsEnabled()

    override fun openNotificationSettings() = context.startActivity(
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    )

    override fun exactAlarmsAllowed() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    override fun openExactAlarmSettings() = context.startActivity(
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
    )

    override fun openFocusModeAccess() =
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))

    /** La ficha de la app en Play; sin Play Store, en el navegador. */
    override fun openStoreListing() {
        val id = context.packageName
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))
        }.recoverCatching {
            val web = Uri.parse("https://play.google.com/store/apps/details?id=$id")
            context.startActivity(Intent(Intent.ACTION_VIEW, web))
        }
    }
}

@Composable
actual fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
actual fun rememberPurchaseHost(): PurchaseHost? = LocalActivity.current

actual val canBlur: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

actual val hasWallpaperColors: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

actual val hasFocusMode: Boolean = true

actual val automaticBackupText: StringResource = Res.string.backup_automatic

actual val storeUnavailableText: StringResource = Res.string.alert_google_play

actual val restoreNothingText: StringResource = Res.string.restore_nothing

actual val proRestoreNothingText: StringResource = Res.string.pro_restore_nothing

actual val proSettingsText: StringResource = Res.string.pro_settings_text

actual val notificationsTitle: StringResource = Res.string.notifications_title

actual val notificationsText: StringResource = Res.string.notifications_text

actual val notificationsOffText: StringResource = Res.string.notifications_off

@Composable
actual fun rememberShowMessage(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { text -> Toast.makeText(context, text, Toast.LENGTH_LONG).show() } }
}

@Composable
actual fun rememberCreateFile(mime: String, onPicked: (PickedFile) -> Unit): (suggestedName: String) -> Unit {
    val picked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(CreateDocument(mime)) { uri -> uri?.let { picked(PickedFile(it)) } }
    return remember(launcher) { { name -> launcher.launch(name) } }
}

@Composable
actual fun rememberOpenFile(mimeTypes: List<String>, onPicked: (PickedFile) -> Unit): () -> Unit {
    val picked by rememberUpdatedState(onPicked)
    val launcher = rememberLauncherForActivityResult(OpenDocument()) { uri -> uri?.let { picked(PickedFile(it)) } }
    return remember(launcher, mimeTypes) { { launcher.launch(mimeTypes.toTypedArray()) } }
}

@Composable
actual fun is24HourClock(): Boolean = DateFormat.is24HourFormat(LocalContext.current)
