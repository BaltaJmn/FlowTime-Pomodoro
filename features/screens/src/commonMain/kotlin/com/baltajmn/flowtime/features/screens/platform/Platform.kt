package com.baltajmn.flowtime.features.screens.platform

import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.data.backup.PickedFile
import com.baltajmn.flowtime.data.pro.PurchaseHost
import org.jetbrains.compose.resources.StringResource

/** El permiso para avisar. [granted] se vuelve a mirar en cada llamada: el usuario lo cambia fuera. */
class NotificationPermission(val granted: () -> Boolean, val ask: () -> Unit)

@Composable
expect fun rememberNotificationPermission(): NotificationPermission

/**
 * Lo que los ajustes piden al sistema. Lo que no existe en una plataforma dice que está bien, para
 * que no salga ningún aviso, y no hace nada.
 */
interface SystemSettings {
    fun notificationsAllowed(): Boolean

    fun openNotificationSettings()

    fun exactAlarmsAllowed(): Boolean

    fun openExactAlarmSettings()

    fun openFocusModeAccess()

    /** La ficha de la tienda, para valorar la app. */
    fun openStoreListing()
}

@Composable
expect fun rememberSystemSettings(): SystemSettings

/** Con [active], la pantalla no se apaga sola. */
@Composable
expect fun KeepScreenOn(active: Boolean)

/** Desde dónde se compra, o null si todavía no hay ventana. */
@Composable
expect fun rememberPurchaseHost(): PurchaseHost?

/** Si se puede difuminar con Modifier.blur: desde Android 12, y en el iPhone. */
expect val canBlur: Boolean

/** Los colores del fondo de pantalla: desde Android 12. */
expect val hasWallpaperColors: Boolean

/** No molestar mientras se trabaja (#43): solo Android deja que una app lo encienda. */
expect val hasFocusMode: Boolean

/** Lo que dice Ajustes de la copia automática del sistema: la de Google en Android, la de iCloud en el iPhone. */
expect val automaticBackupText: StringResource

/** La tienda no responde: Google Play en Android, la App Store en el iPhone. */
expect val storeUnavailableText: StringResource

/** Restaurar compras sin encontrar ninguna: en la cuenta de Google, o en la cuenta de Apple. */
expect val restoreNothingText: StringResource

/** Lo mismo, desde la pantalla de Pro. */
expect val proRestoreNothingText: StringResource

/** Lo que trae Pro, en Ajustes: en el iPhone, sin No molestar ([hasFocusMode]). */
expect val proSettingsText: StringResource

/**
 * Para qué se piden las notificaciones. En Android la sesión va en una notificación; en el iPhone va en
 * la Live Activity, que no las necesita, y son para el aviso de fin de fase.
 */
expect val notificationsTitle: StringResource

expect val notificationsText: StringResource

/** Ajustes, con las notificaciones apagadas. */
expect val notificationsOffText: StringResource

/** Un aviso corto que se va solo: un Toast en Android. */
@Composable
expect fun rememberShowMessage(): (String) -> Unit

/** Abre el selector para guardar un fichero nuevo, con [mime] y el nombre que se le pase. */
@Composable
expect fun rememberCreateFile(mime: String, onPicked: (PickedFile) -> Unit): (suggestedName: String) -> Unit

/** Abre el selector para elegir un fichero de alguno de [mimeTypes]. */
@Composable
expect fun rememberOpenFile(mimeTypes: List<String>, onPicked: (PickedFile) -> Unit): () -> Unit

/** Si el reloj del móvil va en 24 horas. */
@Composable
expect fun is24HourClock(): Boolean
