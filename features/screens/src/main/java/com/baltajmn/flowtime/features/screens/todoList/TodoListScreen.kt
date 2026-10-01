package com.baltajmn.flowtime.features.screens.todoList

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.design.extensions.readableWidth
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.data.pro.Limits
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.features.screens.pro.ProFeature
import com.baltajmn.flowtime.features.screens.pro.ProLauncher
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun TodoListScreen(
    viewModel: TodoListViewModel = koinViewModel(),
    listState: LazyListState,
    proLauncher: ProLauncher = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedTodoListContent(
        state = state,
        listState = listState,
        viewModel = viewModel,
        onSeePro = { proLauncher.open(ProFeature.TASKS) }
    )
}

@Composable
fun AnimatedTodoListContent(
    state: TodoListState,
    listState: LazyListState,
    viewModel: TodoListViewModel,
    onSeePro: () -> Unit
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
                onSeePro = onSeePro
            )
        }
    }
}

@Composable
fun TodoListContent(
    state: TodoListState,
    listState: LazyListState,
    viewModel: TodoListViewModel,
    onSeePro: () -> Unit
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    // La que se está editando, o null para una nueva.
    var editing by remember { mutableStateOf<Task?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val deletedText = stringResource(Res.string.task_deleted)
    val undoText = stringResource(Res.string.task_undo)

    if (showDialog) {
        ItemDialog(
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
        val result = snackbar.showSnackbar(deletedText, undoText, duration = SnackbarDuration.Short)
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

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.Top,
            contentPadding = PaddingValues(24.dp),
            modifier = Modifier
                .fillMaxSize()
                .readableWidth()
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                ScreenTitleWithIcon(
                    text = stringResource(Res.string.todo_list_title),
                    onIconClick = remember {
                        {
                            editing = null
                            showDialog = true
                        }
                    }
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
            item {
                TodoListDay(
                    selectedDate = state.selectedDateToShow,
                    plusWeek = viewModel::plusDay,
                    minusWeek = viewModel::minusDay
                )
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
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
                            editing = selected
                            showDialog = true
                        }
                    },
                    onDeleteClick = viewModel::onDeleteItem,
                    onMoveToToday = viewModel::moveToToday
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }
}

@Composable
fun TodoItem(
    item: Task,
    daysLate: Long = 0,
    onItemClick: (Task) -> Unit,
    onEditClick: (Task) -> Unit,
    onDeleteClick: (Task) -> Unit,
    onMoveToToday: (Task) -> Unit = {}
) {
    val textStyle = if (item.done) {
        Title.copy(textDecoration = TextDecoration.LineThrough)
    } else {
        Title
    }

    val textStyleDescription = if (item.done) {
        SubBody.copy(textDecoration = TextDecoration.LineThrough)
    } else {
        SubBody
    }

    val textColor = if (item.done) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.primary
    }

    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick.invoke(item) }
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(0.8f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = item.title,
                    style = textStyle,
                    color = textColor
                )
                Text(
                    text = item.description,
                    style = textStyleDescription,
                    color = textColor
                )
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

            IconButton(
                modifier = Modifier.weight(0.1f),
                onClick = { onEditClick.invoke(item) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(Res.string.cd_edit_task),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                modifier = Modifier.weight(0.1f),
                onClick = { onDeleteClick.invoke(item) }
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(Res.string.cd_delete_task),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
fun TodoListDay(
    selectedDate: String,
    plusWeek: () -> Unit,
    minusWeek: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { minusWeek.invoke() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = selectedDate,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    style = LargeTitle.copy(
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                )

                IconButton(onClick = { plusWeek.invoke() }) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ScreenTitleWithIcon(text: String, onIconClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(0.1f))
        Text(
            modifier = Modifier.weight(0.8f),
            text = text,
            style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Icon(
            modifier = Modifier
                .weight(0.1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button
                ) { onIconClick.invoke() },
            imageVector = Icons.Filled.Add,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(Res.string.todo_add_item)
        )
    }
}

@Composable
fun ItemDialog(
    initialTitle: String = "",
    initialDescription: String = "",
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.todo_add_item)) },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(Res.string.todo_item_title)) }
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(Res.string.todo_item_description)) }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, description) }) {
                Text(stringResource(Res.string.save))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(Res.string.dialog_cancel))
            }
        }
    )
}