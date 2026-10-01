package com.baltajmn.flowtime.features.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.baltajmn.flowtime.core.design.R

/**
 * El icono activo es el activity-alias encendido del manifiesto de la app: se enciende el elegido y se
 * apagan los demás. El cambio espera a que la app pase a segundo plano ([applyPending]): apagar el alias
 * con el que se abrió la app cierra su tarea, y con la app delante se iría de golpe al lanzador.
 */
actual class AppIcons(private val context: Context) {
    private val packages = context.packageManager
    private var pending: AppIcon? = null

    actual fun current(): AppIcon = pending ?: AppIcon.entries.firstOrNull { icon ->
        when (packages.getComponentEnabledSetting(component(icon))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            // Sin tocar nunca, vale el manifiesto: solo el de siempre está encendido.
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.DEFAULT
            else -> false
        }
    } ?: AppIcon.DEFAULT

    actual fun set(icon: AppIcon) {
        pending = icon
    }

    fun applyPending() {
        val icon = pending ?: return
        pending = null
        // Primero el nuevo: la app no se queda ni un momento sin icono en el lanzador.
        (listOf(icon) + AppIcon.entries.filter { it != icon }).forEach {
            packages.setComponentEnabledSetting(
                component(it),
                if (it == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun component(icon: AppIcon) = ComponentName(context.packageName, "com.baltajmn.flowtime.${icon.alias}")
}

private val AppIcon.alias: String
    get() = when (this) {
        AppIcon.DEFAULT -> "MainActivity"
        AppIcon.LAVENDER -> "IconLavender"
        AppIcon.MINT -> "IconMint"
        AppIcon.CORAL -> "IconCoral"
        AppIcon.SAND -> "IconSand"
        AppIcon.NIGHT -> "IconNight"
        AppIcon.CHERRY -> "IconCherry"
    }

private val AppIcon.mipmap: Int
    get() = when (this) {
        AppIcon.DEFAULT -> R.mipmap.ic_launcher_flowtime
        AppIcon.LAVENDER -> R.mipmap.ic_launcher_lavender
        AppIcon.MINT -> R.mipmap.ic_launcher_mint
        AppIcon.CORAL -> R.mipmap.ic_launcher_coral
        AppIcon.SAND -> R.mipmap.ic_launcher_sand
        AppIcon.NIGHT -> R.mipmap.ic_launcher_night
        AppIcon.CHERRY -> R.mipmap.ic_launcher_cherry
    }

internal actual val asksBeforeIconChange = true

@Composable
internal actual fun rememberAppIconImage(icon: AppIcon, px: Int): ImageBitmap {
    val context = LocalContext.current
    return remember(icon, px) { ContextCompat.getDrawable(context, icon.mipmap)!!.toBitmap(px, px).asImageBitmap() }
}
