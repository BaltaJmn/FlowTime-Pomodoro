package com.baltajmn.flowtime.features.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.backup.Backup
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Ajustes › Copia de seguridad: la copia automática de Android y la copia a un fichero, con el
 * selector de archivos del sistema. Siempre gratis.
 */
@Composable
fun BackupCard(
    state: BackupUiState,
    onExport: (Uri) -> Unit,
    onImport: (Uri) -> Unit,
    onConfirmImport: (withSettings: Boolean) -> Unit,
    onCancelImport: () -> Unit,
    onMessageShown: () -> Unit
) {
    val exportFile = rememberLauncherForActivityResult(CreateDocument(JSON)) { uri ->
        uri?.let(onExport)
    }
    val importFile = rememberLauncherForActivityResult(OpenDocument()) { uri -> uri?.let(onImport) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.backup_title),
                style = LargeTitle.copy(
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.backup_automatic),
                style = SubBody.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.primary),
                textAlign = TextAlign.Center
            )
            ButtonRow(text = R.string.backup_export_label, button = R.string.backup_export) {
                exportFile.launch("flowtime-${LocalDate.now()}.json")
            }
            ButtonRow(text = R.string.backup_import_label, button = R.string.backup_import) {
                // Algunos gestores de archivos no marcan bien el tipo de un .json.
                importFile.launch(arrayOf(JSON, "text/plain", "application/octet-stream"))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = state.lastExportAt?.let { stringResource(R.string.backup_last, it.asDate()) }
                    ?: stringResource(R.string.backup_never),
                style = SubBody.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    state.pending?.let { ImportDialog(it, onConfirm = onConfirmImport, onCancel = onCancelImport) }
    state.message?.let { MessageDialog(it, onDismiss = onMessageShown) }
}

@Composable
private fun ImportDialog(backup: Backup, onConfirm: (Boolean) -> Unit, onCancel: () -> Unit) {
    var withSettings by rememberSaveable { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = stringResource(R.string.backup_import_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(
                        R.string.backup_import_message,
                        backup.sessions.size,
                        backup.tasks.size
                    )
                )
                // Los ajustes no se pisan sin preguntar.
                if (backup.settings != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .toggleable(
                                value = withSettings,
                                role = Role.Checkbox,
                                onValueChange = { withSettings = it }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = withSettings, onCheckedChange = null)
                        Text(
                            text = stringResource(R.string.backup_import_settings),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(withSettings) }) {
                Text(text = stringResource(R.string.backup_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(text = stringResource(R.string.dialog_cancel)) }
        }
    )
}

@Composable
private fun MessageDialog(message: BackupMessage, onDismiss: () -> Unit) {
    val text = when (message) {
        BackupMessage.Exported -> stringResource(R.string.backup_exported)
        is BackupMessage.Restored -> stringResource(
            R.string.backup_restored,
            message.result.sessionsAdded,
            message.result.sessionsExisting,
            message.result.tasksAdded,
            message.result.tagsAdded
        )
        BackupMessage.NotABackup -> stringResource(R.string.backup_not_a_backup)
        BackupMessage.TooNew -> stringResource(R.string.backup_too_new)
        BackupMessage.ExportFailed -> stringResource(R.string.backup_failed)
        BackupMessage.ImportFailed -> stringResource(R.string.backup_import_failed)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(text = text) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.dialog_confirm)) }
        }
    )
}

private const val JSON = "application/json"

private fun Long.asDate(): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    .format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))
