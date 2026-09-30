package com.baltajmn.flowtime.features.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.features.screens.pro.ProAccess

/**
 * Los iconos de la app (#56): el de siempre y uno con los colores de cada tema de Pro. Cada uno es un
 * activity-alias del manifiesto de la app, que se llama como [alias].
 */
enum class AppIcon(val theme: AppTheme, @DrawableRes val icon: Int, private val alias: String) {
    DEFAULT(AppTheme.Blue, R.mipmap.ic_launcher_flowtime, "MainActivity"),
    LAVENDER(AppTheme.Lavender, R.mipmap.ic_launcher_lavender, "IconLavender"),
    MINT(AppTheme.Mint, R.mipmap.ic_launcher_mint, "IconMint"),
    CORAL(AppTheme.Coral, R.mipmap.ic_launcher_coral, "IconCoral"),
    SAND(AppTheme.Sand, R.mipmap.ic_launcher_sand, "IconSand"),
    NIGHT(AppTheme.Night, R.mipmap.ic_launcher_night, "IconNight"),
    CHERRY(AppTheme.Cherry, R.mipmap.ic_launcher_cherry, "IconCherry");

    fun component(context: Context) = ComponentName(
        context.packageName,
        "com.baltajmn.flowtime.$alias"
    )
}

/** El icono activo es el alias encendido: se enciende el elegido y se apagan los demás. */
class AppIcons(private val context: Context) {
    private val packages = context.packageManager

    fun current(): AppIcon = AppIcon.entries.firstOrNull { icon ->
        when (packages.getComponentEnabledSetting(icon.component(context))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            // Sin tocar nunca, vale el manifiesto: solo el de siempre está encendido.
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> icon == AppIcon.DEFAULT
            else -> false
        }
    } ?: AppIcon.DEFAULT

    fun set(icon: AppIcon) {
        // Primero el nuevo: la app no se queda ni un momento sin icono en el lanzador.
        (listOf(icon) + AppIcon.entries.filter { it != icon }).forEach {
            packages.setComponentEnabledSetting(
                it.component(context),
                if (it == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}

/**
 * Los iconos en Apariencia, con Pro a la venta. Cambiar se confirma antes: algunos lanzadores quitan el
 * icono de la pantalla de inicio cuando cambia.
 */
@Composable
fun AppIconRow(icon: AppIcon, pro: ProAccess, onIcon: (AppIcon) -> Unit, onLocked: () -> Unit) {
    var asking by rememberSaveable { mutableStateOf<AppIcon?>(null) }
    Text(
        text = stringResource(R.string.appearance_icon),
        style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(AppIcon.entries) { option ->
            val locked = option.theme.pro && pro == ProAccess.LOCKED && option != icon
            AppIconChoice(option, selected = option == icon, locked = locked) {
                when {
                    locked -> onLocked()
                    option != icon -> asking = option
                }
            }
        }
    }
    asking?.let { chosen ->
        AlertDialog(
            onDismissRequest = { asking = null },
            title = { Text(stringResource(R.string.app_icon_change_title)) },
            text = { Text(stringResource(R.string.app_icon_change_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        asking = null
                        onIcon(chosen)
                    }
                ) {
                    Text(stringResource(R.string.app_icon_change))
                }
            },
            dismissButton = {
                TextButton(onClick = { asking = null }) {
                    Text(
                        stringResource(R.string.dialog_cancel)
                    )
                }
            }
        )
    }
}

@Composable
private fun AppIconChoice(icon: AppIcon, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { 44.dp.roundToPx() }
    val image = remember(icon, px) {
        ContextCompat.getDrawable(context, icon.icon)!!.toBitmap(
            px,
            px
        ).asImageBitmap()
    }
    val name = stringResource(icon.theme.label)
    val description = stringResource(
        if (locked) R.string.cd_pro_app_icon else R.string.cd_app_icon,
        name
    )
    Box(
        modifier = Modifier
            .size(52.dp)
            .border(
                3.dp,
                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                CircleShape
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description }
    ) {
        Image(
            bitmap = image,
            contentDescription = null,
            // Redondo, como el aro de elegido: la forma de los iconos cambia de un lanzador a otro.
            modifier = Modifier
                .padding(4.dp)
                .fillMaxSize()
                .clip(CircleShape)
        )
        if (locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lock_on),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
