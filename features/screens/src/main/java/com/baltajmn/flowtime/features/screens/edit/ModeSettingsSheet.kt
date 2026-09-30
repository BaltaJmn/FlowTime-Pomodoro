package com.baltajmn.flowtime.features.screens.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.components.PercentageRange
import com.baltajmn.flowtime.features.screens.common.composable.components.PomodoroRange
import com.baltajmn.flowtime.features.screens.common.composable.components.flowTimeRanges
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/** La configuración del modo elegido y "Continuar después del descanso", en una hoja. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSettingsSheet(
    mode: TimerMode,
    onDismiss: () -> Unit,
    viewModel: EditViewModel = koinViewModel(key = "mode_settings_${mode.name}") {
        parametersOf(
            mode
        )
    }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reload() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        ModeSettingsContent(
            state = state,
            viewModel = viewModel,
            onSave = {
                viewModel.saveChanges()
                onDismiss()
            }
        )
    }
}

@Composable
fun ModeSettingsContent(state: EditState, viewModel: EditViewModel, onSave: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.mode) {
            TimerMode.POMODORO -> pomodoroSettings(state, viewModel)
            TimerMode.FLOW_TIME -> flowTimeSettings(state, viewModel)
            TimerMode.PERCENTAGE -> percentageSettings(state, viewModel)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.pomodoro_continue_after_break),
                    style = SubBody.copy(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.continueAfterBreak,
                    onCheckedChange = viewModel::setContinueAfterBreak
                )
            }
        }

        item {
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.settings_save), style = SubBody)
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

private fun LazyListScope.title(text: Int) = item {
    Text(
        text = stringResource(text),
        style = LargeTitle.copy(fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
    )
}

fun LazyListScope.pomodoroSettings(state: EditState, viewModel: EditViewModel) {
    title(R.string.pomodoro_settings_title)
    item {
        PomodoroRange(
            range = state.pomodoroRange,
            onValueChanged = { range: RangeModel -> viewModel.modifyPomodoro(range) }
        )
    }
}

fun LazyListScope.flowTimeSettings(state: EditState, viewModel: EditViewModel) {
    title(R.string.flow_time_settings_title)
    flowTimeRanges(
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

fun LazyListScope.percentageSettings(state: EditState, viewModel: EditViewModel) {
    title(R.string.percentage_settings_title)
    item {
        PercentageRange(
            percentage = state.percentage,
            onPercentageChange = { percentage -> viewModel.modifyPercentage(percentage) }
        )
    }
}
