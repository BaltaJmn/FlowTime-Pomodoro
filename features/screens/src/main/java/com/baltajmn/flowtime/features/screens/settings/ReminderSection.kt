package com.baltajmn.flowtime.features.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.reminder.Reminder
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.datetime.toKotlinDayOfWeek
import kotlinx.datetime.toJavaLocalTime

/** Ajustes › Recordatorio diario (#45): apagado de entrada; la hora y los días, solo encendido. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderCard(reminder: Reminder, onChange: (Reminder) -> Unit) {
    val context = LocalContext.current
    val askNotifications = rememberLauncherForActivityResult(RequestPermission()) {}
    var pickTime by rememberSaveable { mutableStateOf(false) }

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.reminder_title),
                textAlign = TextAlign.Center,
                style = LargeTitle.copy(fontSize = 25.sp, color = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.reminder_text),
                    modifier = Modifier.weight(1f),
                    style = SubBody.copy(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Switch(
                    checked = reminder.enabled,
                    onCheckedChange = { on ->
                        onChange(reminder.copy(enabled = on))
                        // Encenderlo es cuando se entiende para qué es el permiso.
                        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) ==
                            PackageManager.PERMISSION_GRANTED
                        if (on && !granted) {
                            askNotifications.launch(
                                Manifest.permission.POST_NOTIFICATIONS
                            )
                        }
                    }
                )
            }
            if (reminder.enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { pickTime = true }) {
                    Text(
                        text = reminder.time.toJavaLocalTime().format(
                            DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
                        )
                    )
                }
                val locale = Locale.getDefault()
                val first = WeekFields.of(locale).firstDayOfWeek
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    (0L..6L).map(first::plus).forEach { day ->
                        FilterChip(
                            selected = reminder.on(day.toKotlinDayOfWeek()),
                            onClick = { onChange(reminder.toggle(day.toKotlinDayOfWeek())) },
                            label = { Text(text = day.getDisplayName(TextStyle.SHORT, locale)) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (pickTime) {
        TimeDialog(
            reminder = reminder,
            is24Hour = DateFormat.is24HourFormat(context),
            onPick = { minute ->
                onChange(reminder.copy(minuteOfDay = minute))
                pickTime = false
            },
            onDismiss = { pickTime = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    reminder: Reminder,
    is24Hour: Boolean,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberTimePickerState(reminder.time.hour, reminder.time.minute, is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(state.hour * 60 + state.minute) }) {
                Text(text = stringResource(R.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(R.string.dialog_cancel)) }
        }
    )
}
