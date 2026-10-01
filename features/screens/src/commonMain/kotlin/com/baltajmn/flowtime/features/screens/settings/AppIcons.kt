package com.baltajmn.flowtime.features.screens.settings

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Los iconos de la app (#56): el de siempre y uno con los colores de cada tema de Pro. */
enum class AppIcon(val theme: AppTheme) {
    DEFAULT(AppTheme.Blue),
    LAVENDER(AppTheme.Lavender),
    MINT(AppTheme.Mint),
    CORAL(AppTheme.Coral),
    SAND(AppTheme.Sand),
    NIGHT(AppTheme.Night),
    CHERRY(AppTheme.Cherry)
}

/** El icono del lanzador, o de la pantalla de inicio en el iPhone. */
expect class AppIcons {
    fun current(): AppIcon

    fun set(icon: AppIcon)
}

/** El icono como sale en el lanzador, de [px] de lado. */
@Composable
internal expect fun rememberAppIconImage(icon: AppIcon, px: Int): ImageBitmap

/** Si se pregunta antes de cambiar: el iPhone ya avisa él, con su propia alerta, al cambiarlo. */
internal expect val asksBeforeIconChange: Boolean

/**
 * Los iconos en Apariencia, con Pro a la venta. En Android cambiar se confirma antes: algunos
 * lanzadores quitan el icono de la pantalla de inicio cuando cambia.
 */
@Composable
fun AppIconRow(icon: AppIcon, pro: ProAccess, onIcon: (AppIcon) -> Unit, onLocked: () -> Unit) {
    var asking by rememberSaveable { mutableStateOf<AppIcon?>(null) }
    Text(
        text = stringResource(Res.string.appearance_icon),
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
                    option == icon -> Unit
                    asksBeforeIconChange -> asking = option
                    else -> onIcon(option)
                }
            }
        }
    }
    asking?.let { chosen ->
        AlertDialog(
            onDismissRequest = { asking = null },
            title = { Text(stringResource(Res.string.app_icon_change_title)) },
            text = { Text(stringResource(Res.string.app_icon_change_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        asking = null
                        onIcon(chosen)
                    }
                ) {
                    Text(stringResource(Res.string.app_icon_change))
                }
            },
            dismissButton = {
                TextButton(onClick = { asking = null }) {
                    Text(
                        stringResource(Res.string.dialog_cancel)
                    )
                }
            }
        )
    }
}

@Composable
private fun AppIconChoice(icon: AppIcon, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val px = with(LocalDensity.current) { 44.dp.roundToPx() }
    val image = rememberAppIconImage(icon, px)
    val name = stringResource(icon.theme.label)
    val description = stringResource(
        if (locked) Res.string.cd_pro_app_icon else Res.string.cd_app_icon,
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
                    painter = painterResource(Res.drawable.ic_lock_on),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
