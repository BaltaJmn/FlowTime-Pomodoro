package com.baltajmn.flowtime.features.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.Blue
import com.baltajmn.flowtime.core.design.theme.DarkBlue
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.LightBlue
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.features.screens.settings.components.FlowTimeRanges
import com.baltajmn.flowtime.features.screens.settings.components.PomodoroRange
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    listState: LazyListState
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedSettingsContent(
        state = state,
        listState = listState,
        viewModel = viewModel
    )
}

@Composable
fun AnimatedSettingsContent(
    state: SettingsState,
    listState: LazyListState,
    viewModel: SettingsViewModel
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
            )
        }
    }
}

@Composable
fun SettingsContent(
    state: SettingsState,
    listState: LazyListState,
    viewModel: SettingsViewModel
) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(LightBlue)
    ) {
        item { Spacer(modifier = Modifier.height(64.dp)) }
        item {
            Text(
                text = "Flow Time settings", style = LargeTitle.copy(fontSize = 30.sp, color = Blue)
            )
        }
        FlowTimeRanges(
            ranges = state.flowTimeRanges,
            onValueChanged = { index, range ->
                viewModel.modifyRange(
                    index = index,
                    range = range
                )
            },
            onDeleteClicked = { index -> viewModel.deleteRange(index = index) },
            onAddRangeClicked = { viewModel.addRange() }
        )
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Text(
                text = "Pomodoro settings", style = LargeTitle.copy(fontSize = 30.sp, color = Blue)
            )
        }
        item {
            PomodoroRange(
                range = state.pomodoroRange,
                onValueChanged = { range ->
                    viewModel.modifyPomodoro(range)
                }
            )
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
        item {
            ButtonSave {
                viewModel.saveChanges()
            }
        }

        item {
            ButtonSave {
                viewModel.changeColor()
            }
        }
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
            Text(text = "Save Changes", style = SubBody.copy(color = DarkBlue))
        }
    }
}

