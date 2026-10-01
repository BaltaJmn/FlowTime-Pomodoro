package com.baltajmn.flowtime.features.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.TagPalette
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * Ajustes › Etiquetas: añadir, cambiar el nombre o el color, archivar y recuperar. Lo justo para
 * usarlas hasta que se rehagan las pantallas; ordenarlas arrastrando llegará entonces.
 */
@Composable
fun TagsCard(
    state: TagsUiState,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onRecolor: (Tag) -> Unit,
    onArchive: (Long) -> Unit,
    onUnarchive: (Long) -> Unit,
    onMessageShown: () -> Unit,
    onSeePro: () -> Unit
) {
    // La que se está renombrando, o NEW para una nueva.
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    SettingsCard(Res.string.tags_title) {
        Spacer(modifier = Modifier.height(4.dp))
        state.active.forEach { tag ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val description = stringResource(Res.string.tag_color)
                IconButton(
                    onClick = { onRecolor(tag) },
                    modifier = Modifier.semantics { contentDescription = description }
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(TagPalette.color(tag.color, dark), CircleShape)
                    )
                }
                Text(
                    text = tag.name,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { editing = tag.id }
                        .padding(vertical = 12.dp),
                    style = SubBody.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                TextButton(onClick = { onArchive(tag.id) }) {
                    Text(text = stringResource(Res.string.tag_archive))
                }
            }
        }
        TextButton(onClick = { editing = NEW }) { Text(text = stringResource(Res.string.tag_add)) }
        if (state.archived.isNotEmpty()) {
            Text(
                text = stringResource(Res.string.tags_archived),
                style = SubBody.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            state.archived.forEach { tag ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.name,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                        style = SubBody.copy(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    TextButton(onClick = { onUnarchive(tag.id) }) {
                        Text(text = stringResource(Res.string.tag_unarchive))
                    }
                }
            }
        }
    }

    editing?.let { id ->
        NameDialog(
            initial = state.active.firstOrNull { it.id == id }?.name.orEmpty(),
            onConfirm = { name ->
                if (id == NEW) onAdd(name) else onRename(id, name)
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = onMessageShown,
            text = {
                Text(
                    text = when (message) {
                        TagMessage.EMPTY -> stringResource(Res.string.tag_empty)
                        TagMessage.DUPLICATE -> stringResource(Res.string.tag_duplicate)
                        TagMessage.LIMIT -> stringResource(Res.string.tag_limit, Limits.FREE_TAGS)
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = onMessageShown) {
                    Text(text = stringResource(Res.string.dialog_confirm))
                }
            },
            dismissButton = {
                if (message == TagMessage.LIMIT) {
                    TextButton(
                        onClick = {
                            onMessageShown()
                            onSeePro()
                        }
                    ) {
                        Text(text = stringResource(Res.string.pro_see))
                    }
                }
            }
        )
    }
}

@Composable
private fun NameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val title = if (initial.isEmpty()) Res.string.tag_add else Res.string.tag_rename
            Text(text = stringResource(title))
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = stringResource(Res.string.tag_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) {
                Text(text = stringResource(Res.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.dialog_cancel)) }
        }
    )
}

private const val NEW = -1L
