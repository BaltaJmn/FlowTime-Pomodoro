package com.baltajmn.flowtime.features.screens.platform

import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.data.backup.PickedFile
import com.baltajmn.flowtime.data.pro.PurchaseHost

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

    /** No molestar mientras se trabaja (#43): solo Android deja que una app lo encienda. */
    val hasFocusMode: Boolean

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

/** Cambiar el icono de la app (#56): de momento, solo en Android. */
expect val hasAppIcons: Boolean

/** Guardar y abrir ficheros con el selector del sistema (copia de seguridad, CSV): de momento, solo en Android. */
expect val hasFiles: Boolean

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
