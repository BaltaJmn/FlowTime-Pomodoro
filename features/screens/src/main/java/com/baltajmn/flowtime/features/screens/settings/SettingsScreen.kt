package com.baltajmn.flowtime.features.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.features.screens.components.FlowTimeRanges
import com.baltajmn.flowtime.features.screens.components.PomodoroRange
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    listState: LazyListState,
    navigateToHistory: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedSettingsContent(
        state = state,
        listState = listState,
        viewModel = viewModel,
        navigateToHistory = navigateToHistory,
        onThemeChanged = onThemeChanged
    )
}

@Composable
fun AnimatedSettingsContent(
    state: SettingsState,
    listState: LazyListState,
    viewModel: SettingsViewModel,
    navigateToHistory: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) {
        when (it) {
            true -> LoadingView()
            false -> SettingsContent(
                state = state,
                listState = listState,
                viewModel = viewModel,
                navigateToHistory = navigateToHistory,
                onThemeChanged = onThemeChanged
            )
        }
    }
}

@Composable
fun SettingsContent(
    state: SettingsState,
    listState: LazyListState,
    viewModel: SettingsViewModel,
    navigateToHistory: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit
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
                text = LocalContext.current.getString(R.string.flow_time_settings_title),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        FlowTimeRanges(
            ranges = state.flowTimeRanges,
            onValueChanged = { index: Int, range: RangeModel ->
                viewModel.modifyRange(
                    index = index,
                    range = range
                )
            },
            onDeleteClicked = { index: Int -> viewModel.deleteRange(index = index) },
            onAddRangeClicked = { viewModel.addRange() }
        )
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.pomodoro_settings_title),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        item {
            PomodoroRange(
                range = state.pomodoroRange,
                onValueChanged = { range: RangeModel ->
                    viewModel.modifyPomodoro(range)
                }
            )
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.theme_settings_title),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            LazyRow(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(AppTheme.entries) { color ->
                    Box(
                        modifier = Modifier
                            .background(color = color.color)
                            .aspectRatio(1f)
                            .size(32.dp)
                            .semantics { contentDescription = "Color $color" }
                            .clickable {
                                onThemeChanged.invoke(color)
                                viewModel.saveColor(color)
                            }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            ButtonSave {
                viewModel.saveChanges()
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.others),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { ButtonHistory(navigateToHistory = navigateToHistory) }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
fun ButtonSave(onSaveClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { onSaveClicked.invoke() }) {
            Text(
                text = LocalContext.current.getString(R.string.settings_save),
                style = SubBody.copy(color = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
fun ButtonHistory(navigateToHistory: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = LocalContext.current.getString((R.string.study_history)),
            style = SubBody.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigateToHistory.invoke() }
        ) {
            Text(
                text = LocalContext.current.getString(R.string.go_to_history),
                style = SubBody.copy(color = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}
