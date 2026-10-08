package com.baltajmn.flowtime.features.screens.common.composable.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.material3.Icon
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
 * La tarea de la sesión (#40), encima de empezar: primero en qué, luego a trabajar. Sin elegir,
 * pregunta; empezar sin tarea sigue siendo un solo toque.
 */
@Composable
fun TaskChip(
    title: String?,
    pending: List<Task>,
    onSelect: (Task?) -> Unit,
    /** Crea una tarea para hoy y la elige; null, sin esa opción. */
    onCreate: ((String) -> Unit)? = null
) {
    var picking by rememberSaveable { mutableStateOf(false) }
    AssistChip(
        onClick = { picking = true },
        modifier = Modifier.widthIn(max = 320.dp),
        label = {
            Text(
                text = if (title != null) {
                    stringResource(Res.string.task_chip, title)
                } else {
                    stringResource(Res.string.task_pick_title)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        // La flecha dice que se elige de una lista.
        trailingIcon = { Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null) }
    )
    if (picking) {
        TaskPicker(
            pending = pending,
            onSelect = { task ->
                picking = false
                onSelect(task)
            },
            onCreate = onCreate?.let { create ->
                { newTitle: String ->
                    picking = false
                    create(newTitle)
                }
            },
            onDismiss = { picking = false }
        )
    }
}

/** Las pendientes de hoy y, debajo, escribir una nueva: sin tareas, antes solo quedaba "Sin tarea". */
@Composable
private fun TaskPicker(
    pending: List<Task>,
    onSelect: (Task?) -> Unit,
    onCreate: ((String) -> Unit)?,
    onDismiss: () -> Unit
) {
    var newTitle by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.task_pick_title)) },
        text = {
            // Con quince tareas no cabían, y las de abajo no se podían elegir.
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (pending.isEmpty()) Text(text = stringResource(Res.string.task_no_pending))
                pending.forEach { task ->
                    TextButton(
                        onClick = { onSelect(task) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(text = task.title, modifier = Modifier.fillMaxWidth()) }
                }
                onCreate?.let { create ->
                    TextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(text = stringResource(Res.string.todo_add_item)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { if (newTitle.isNotBlank()) create(newTitle) }),
                        trailingIcon = {
                            IconButton(onClick = { create(newTitle) }, enabled = newTitle.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = stringResource(Res.string.todo_add_item)
                                )
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(null) }) { Text(text = stringResource(Res.string.task_none)) }
        }
    )
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
