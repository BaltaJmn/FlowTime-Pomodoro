package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.stringResource

/**
 * La tarea de la sesión (#40): un chip opcional junto a las etiquetas. Empezar sin tarea sigue
 * siendo un solo toque.
 */
@Composable
fun TaskChip(title: String?, pending: List<Task>, onSelect: (Task?) -> Unit) {
    var picking by rememberSaveable { mutableStateOf(false) }
    AssistChip(
        onClick = { picking = true },
        label = {
            Text(
                text = if (title != null) {
                    stringResource(Res.string.task_chip, title)
                } else {
                    stringResource(Res.string.task_none)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    )
    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text(text = stringResource(Res.string.task_pick_title)) },
            text = {
                Column {
                    if (pending.isEmpty()) Text(text = stringResource(Res.string.task_no_pending))
                    pending.forEach { task ->
                        TextButton(
                            onClick = {
                                onSelect(task)
                                picking = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(text = task.title, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSelect(null)
                        picking = false
                    }
                ) { Text(text = stringResource(Res.string.task_none)) }
            }
        )
    }
}

/** En el descanso de un bloque con tarea: "¿Has terminado «tarea»?". */
@Composable
fun TaskDoneQuestion(title: String, onYes: () -> Unit, onNotYet: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.task_done_question, title),
            style = SubBody.copy(color = MaterialTheme.colorScheme.onSurface),
            textAlign = TextAlign.Center
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onNotYet) { Text(text = stringResource(Res.string.task_done_not_yet)) }
            TextButton(onClick = onYes) { Text(text = stringResource(Res.string.task_done_yes)) }
        }
    }
}
