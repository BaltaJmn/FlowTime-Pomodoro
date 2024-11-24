package com.baltajmn.flowtime.features.screens.todoList

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.common.model.ListItem
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.Title
import org.koin.androidx.compose.koinViewModel

@Composable
fun TodoListScreen(
    viewModel: TodoListViewModel = koinViewModel(),
    listState: LazyListState
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedTodoListContent(
        state = state,
        listState = listState,
        viewModel = viewModel
    )
}

@Composable
fun AnimatedTodoListContent(
    state: TodoListState,
    listState: LazyListState,
    viewModel: TodoListViewModel
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) {
        when (it) {
            true -> LoadingView()
            false -> TodoListContent(
                state = state,
                listState = listState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun TodoListContent(
    state: TodoListState,
    listState: LazyListState,
    viewModel: TodoListViewModel
) {
    var showDialog by remember { mutableStateOf(false) }
    var isEdit by remember { mutableStateOf(false) }
    var currentListItem by remember { mutableStateOf(ListItem()) }

    if (showDialog) {
        ItemDialog(
            item = currentListItem,
            onDismiss = { showDialog = false },
            onSave = { title, description ->
                showDialog = false
                if (isEdit) {
                    viewModel.onUpdateItem(
                        currentListItem.copy(
                            title = title,
                            description = description
                        )
                    )
                } else {
                    viewModel.onAddItem(title, description)
                }
            }
        )
    }

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            ScreenTitleWithIcon(
                text = "Todo List",
                onIconClick = {
                    currentListItem = ListItem()
                    isEdit = false
                    showDialog = true
                }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            TodoListDay(
                selectedDate = state.selectedDateToShow,
                plusWeek = { viewModel.plusDay() },
                minusWeek = { viewModel.minusDay() }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            TodoItemList(
                itemList = state.currentTodoList.todoList,
                onItemClick = {
                    viewModel.markAsDone(it)
                },
                onEditClick = {
                    currentListItem = it
                    isEdit = true
                    showDialog = true
                },
                onDeleteClick = {
                    viewModel.onDeleteItem(it)
                }
            )
        }

    }
}

context(LazyListScope)
@Composable
fun TodoItemList(
    itemList: List<ListItem>,
    onItemClick: (ListItem) -> Unit,
    onEditClick: (ListItem) -> Unit,
    onDeleteClick: (ListItem) -> Unit
) {
    items(itemList) { item ->
        TodoItem(
            item = item,
            onItemClick = onItemClick,
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick
        )
    }
}

@Composable
fun TodoItem(
    item: ListItem,
    onItemClick: (ListItem) -> Unit,
    onEditClick: (ListItem) -> Unit,
    onDeleteClick: (ListItem) -> Unit
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
        }

        IconButton(
            modifier = Modifier.weight(0.1f),
            onClick = { onEditClick.invoke(item) }
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(
            modifier = Modifier.weight(0.1f),
            onClick = { onDeleteClick.invoke(item) }
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TodoListDay(
    selectedDate: String,
    plusWeek: () -> Unit,
    minusWeek: () -> Unit
) {
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
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
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
                    indication = null
                ) { onIconClick.invoke() },
            imageVector = Icons.Filled.Add,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )
    }
}

@Composable
fun ItemDialog(
    item: ListItem = ListItem(),
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add Item") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") }
                )
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, description) }) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}