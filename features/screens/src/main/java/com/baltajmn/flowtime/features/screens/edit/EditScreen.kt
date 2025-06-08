package com.baltajmn.flowtime.features.screens.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.features.screens.common.composable.components.FlowTimeRanges
import com.baltajmn.flowtime.features.screens.common.composable.components.PercentageRange
import com.baltajmn.flowtime.features.screens.common.composable.components.PomodoroRange
import org.koin.androidx.compose.koinViewModel

@Composable
fun EditScreen(
    viewModel: EditViewModel = koinViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(64.dp)) }

        when (state.screenType) {
            ScreenType.Pomodoro -> PomodoroSettings(state, viewModel)
            ScreenType.FlowTime -> FlowTimeSettings(state, viewModel)
            ScreenType.Percentage -> PercentageSettings(state, viewModel)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        item {
            ButtonSave {
                viewModel.saveChanges()
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }

        Advantages(screenType = state.screenType)
    }
}

fun LazyListScope.PomodoroSettings(state: EditState, viewModel: EditViewModel) {
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
}

fun LazyListScope.FlowTimeSettings(state: EditState, viewModel: EditViewModel) {
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
}

fun LazyListScope.PercentageSettings(state: EditState, viewModel: EditViewModel) {
    item {
        Text(
            text = LocalContext.current.getString(R.string.percentage_settings_title),
            style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        )
    }
    item {
        PercentageRange(
            percentage = state.percentage,
            onPercentageChange = { percentage ->
                viewModel.modifyPercentage(percentage)
            }
        )
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
                style = SubBody
            )
        }
    }
}

fun LazyListScope.Advantages(screenType: ScreenType) {
    item {
        Text(
            text = LocalContext.current.getString(R.string.advantage_title),
            style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        )
    }

    item { Spacer(modifier = Modifier.height(16.dp)) }

    item {
        val text = when (screenType) {
            ScreenType.Pomodoro -> LocalContext.current.getString(R.string.pomodoro_advantages)
            ScreenType.FlowTime -> LocalContext.current.getString(R.string.flow_time_advantages)
            ScreenType.Percentage -> LocalContext.current.getString(R.string.percentage_advantages)
        }

        Text(
            text = text,
            style = SmallTitle.copy(
                color = MaterialTheme.colorScheme.tertiary
            )
        )
    }
}