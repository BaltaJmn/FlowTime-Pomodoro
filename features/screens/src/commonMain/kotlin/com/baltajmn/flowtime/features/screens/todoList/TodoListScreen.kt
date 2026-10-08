package com.baltajmn.flowtime.features.screens.todoList

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.components.FlowCard
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.extensions.readableWidth
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.features.screens.common.composable.components.ScreenTitle
import com.baltajmn.flowtime.features.screens.pro.ProFeature
import com.baltajmn.flowtime.features.screens.pro.ProLauncher
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TodoListScreen(
    viewModel: TodoListViewModel = koinViewModel(),
    listState: LazyListState,
    /** Lleva a la pantalla de concentración, con la tarea ya elegida. */
    onOpenFocus: () -> Unit = {},
    engine: FocusEngine = koinInject(),
    proLauncher: ProLauncher = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedTodoListContent(
        state = state,
        listState = listState,
        viewModel = viewModel,
        onSeePro = { proLauncher.open(ProFeature.TASKS) },
        onFocus = { task ->
            engine.setTask(task.id, task.tagId, task.title)
            onOpenFocus()
        }
    )
}

@Composable
fun AnimatedTodoListContent(
    state: TodoListState,
    listState: LazyListState,
    viewModel: TodoListViewModel,
    onSeePro: () -> Unit,
    onFocus: ((Task) -> Unit)? = null
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = "todo_list_loading"
    ) { isLoading ->
        if (isLoading) {
            LoadingView()
        } else {
            TodoListContent(
                state = state,
                listState = listState,
                viewModel = viewModel,
                onSeePro = onSeePro,
                onFocus = onFocus
            )
        }
    }
}

@Composable
fun TodoListContent(
    state: TodoListState,
    listState: LazyListState,
    viewModel: TodoListViewModel,
    onSeePro: () -> Unit,
    /** Empezar a trabajar en una tarea desde la lista; null, sin ese botón. */
    onFocus: ((Task) -> Unit)? = null
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    // La que se está editando, o null para una nueva. Por id, para que sobreviva a girar la pantalla.
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val editing = state.tasks.firstOrNull { it.id == editingId }
    val snackbar = remember { SnackbarHostState() }
    val deletedText = stringResource(Res.string.task_deleted)
    val undoText = stringResource(Res.string.task_undo)
    val add = {
        editingId = null
        showDialog = true
    }

    if (showDialog) {
        ItemDialog(
            editing = editing != null,
            initialTitle = editing?.title.orEmpty(),
            initialDescription = editing?.description.orEmpty(),
            onDismiss = { showDialog = false },
            onSave = { title, description ->
                showDialog = false
                editing?.let { viewModel.onUpdateItem(it, title, description) }
                    ?: viewModel.onAddItem(title, description)
            }
        )
    }

    // Borrar no pregunta: se puede deshacer.
    LaunchedEffect(state.deleted) {
        if (state.deleted == null) return@LaunchedEffect
        val result = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.onDeleteShown()
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::onMessageShown,
            text = {
                Text(
                    text = when (message) {
                        TaskMessage.EMPTY -> stringResource(Res.string.task_empty)
                        TaskMessage.LIMIT -> stringResource(
                            Res.string.task_limit,
                            Limits.FREE_PENDING_TASKS
                        )
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::onMessageShown) {
                    Text(text = stringResource(Res.string.dialog_confirm))
                }
            },
            dismissButton = {
                if (message == TaskMessage.LIMIT) {
                    TextButton(
                        onClick = {
                            viewModel.onMessageShown()
                            onSeePro()
                        }
                    ) {
                        Text(text = stringResource(Res.string.pro_see))
                    }
                }
            }
        )
    }

    // Añadir abajo, al alcance del pulgar; el aviso de deshacer sale encima del botón.
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = add) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = stringResource(Res.string.todo_add_item))
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0)
    ) { padding ->
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .readableWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            item { ScreenTitle(text = stringResource(Res.string.nav_todo_list)) }
            item {
                TodoListDay(
                    selectedDate = state.selectedDateToShow,
                    plusWeek = viewModel::plusDay,
                    minusWeek = viewModel::minusDay,
                    onToday = if (state.selectedDate != state.today) viewModel::goToday else null
                )
            }
            if (state.tasks.isEmpty()) {
                item { EmptyDay(onAdd = add) }
            }
            items(
                items = state.tasks,
                key = { it.id }
            ) { task ->
                TodoItem(
                    item = task,
                    // Solo hoy se ven las que vienen de días anteriores.
                    daysLate = if (state.selectedDate == state.today) task.daysLate(state.today) else 0,
                    onItemClick = viewModel::markAsDone,
                    onEditClick = remember {
                        { selected ->
                            editingId = selected.id
                            showDialog = true
                        }
                    },
                    onDeleteClick = viewModel::onDeleteItem,
                    onMoveToToday = viewModel::moveToToday,
                    onFocus = onFocus
                )
            }
        }
    }
}

/** Un día sin tareas: lo dice y deja añadir una ahí mismo. */
@Composable
private fun EmptyDay(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.todo_empty),
            style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.Center
        )
        TextButton(onClick = onAdd) { Text(text = stringResource(Res.string.todo_add_item)) }
    }
}

/**
 * Una tarea. Toda la fila marca y desmarca, con la casilla a la vista: sin ella, tocar la tarea la
 * tachaba sin aviso. Editar y borrar van en el menú: juntos y del mismo tamaño se confundían.
 */
@Composable
fun TodoItem(
    item: Task,
    daysLate: Long = 0,
    onItemClick: (Task) -> Unit,
    onEditClick: (Task) -> Unit,
    onDeleteClick: (Task) -> Unit,
    onMoveToToday: (Task) -> Unit = {},
    onFocus: ((Task) -> Unit)? = null
) {
    val decoration = if (item.done) TextDecoration.LineThrough else null
    // Hecha, en el gris del texto secundario: con transparencia se quedaba por debajo del contraste mínimo.
    val textColor = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    var menu by remember { mutableStateOf(false) }

    FlowCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = item.done, role = Role.Checkbox, onValueChange = { onItemClick(item) })
                .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.done, onCheckedChange = null, modifier = Modifier.padding(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = item.title,
                    style = SmallTitle.copy(textDecoration = decoration),
                    color = textColor
                )
                if (item.description.isNotBlank()) {
                    Text(
                        text = item.description,
                        style = SubBody.copy(textDecoration = decoration),
                        color = textColor
                    )
                }
                // El tiempo que se le ha dedicado, sumando sus sesiones (#40).
                if (item.focusSeconds >= 60) {
                    Text(
                        text = (item.focusSeconds / 60).formatMinutesStudying(),
                        style = SubBody.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (daysLate > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (daysLate == 1L) {
                                stringResource(Res.string.task_late_yesterday)
                            } else {
                                pluralStringResource(
                                    Res.plurals.task_late_days,
                                    daysLate.toInt(),
                                    daysLate.toInt()
                                )
                            },
                            style = SubBody.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { onMoveToToday(item) }) {
                            Text(text = stringResource(Res.string.task_move_today))
                        }
                    }
                }
            }

            if (onFocus != null && !item.done) {
                IconButton(onClick = { onFocus(item) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_timer),
                        contentDescription = stringResource(Res.string.cd_focus_task, item.title),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = stringResource(Res.string.more_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(Res.string.cd_edit_task)) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Edit, contentDescription = null) },
                        onClick = {
                            menu = false
                            onEditClick(item)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(Res.string.cd_delete_task)) },
                        leadingIcon = { Icon(imageVector = Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            menu = false
                            onDeleteClick(item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TodoListDay(
    selectedDate: String,
    plusWeek: () -> Unit,
    minusWeek: () -> Unit,
    /** Volver a hoy de un toque, lejos de hoy; null, en hoy. */
    onToday: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { minusWeek.invoke() }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(Res.string.todo_previous_day),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = selectedDate,
                style = SmallTitle.copy(
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            )
            onToday?.let { TextButton(onClick = it) { Text(text = stringResource(Res.string.stats_today)) } }
        }

        IconButton(onClick = { plusWeek.invoke() }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(Res.string.todo_next_day),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Añadir o editar. Guardar no se puede sin título: antes salía un error después de pulsarlo. */
@Composable
fun ItemDialog(
    editing: Boolean = false,
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var description by rememberSaveable { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(if (editing) Res.string.cd_edit_task else Res.string.todo_add_item)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.todo_item_title)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.todo_item_description)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(title, description) }, enabled = title.isNotBlank()) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        }
    )
}
