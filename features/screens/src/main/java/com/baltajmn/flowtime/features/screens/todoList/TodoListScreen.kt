package com.baltajmn.flowtime.features.screens.todoList

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.common.model.ListItem
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.LargeTitle
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
            Text(
                text = "Todo List",
                style = LargeTitle.copy(
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.primary
                ),
                color = MaterialTheme.colorScheme.primary
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
                onItemClick = { /*TODO*/ }
            )
        }

    }
}

context(LazyListScope)
@Composable
fun TodoItemList(
    itemList: List<ListItem>,
    onItemClick: (ListItem) -> Unit
) {
    items(itemList) { item ->
        TodoItem(
            item = item,
            onItemClick = onItemClick
        )
    }
}

@Composable
fun TodoItem(
    item: ListItem,
    onItemClick: (ListItem) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick.invoke(item) }
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = item.title,
            style = Title,
            color = MaterialTheme.colorScheme.primary
        )
        IconButton(onClick = { /*TODO*/ }) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
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