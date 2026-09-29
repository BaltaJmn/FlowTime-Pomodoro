package com.baltajmn.flowtime.features.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PERCENTAGE_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.features.screens.flowtime.withCumulativeTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class EditViewModel(
    savedStateHandle: SavedStateHandle,
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider
) : ViewModel() {

    private val type: ScreenType = ScreenType.valueOf(checkNotNull(savedStateHandle["type"]))

    private val _uiState = MutableStateFlow(EditState())
    val uiState: StateFlow<EditState> = _uiState

    init {
        _uiState.update {
            it.copy(
                screenType = type,
                flowTimeRanges = (
                    dataProvider.getRangeModelList(FLOW_TIME_RANGE)
                        ?: TimerDefaults.flowTimeRanges()
                    ).withCumulativeTotals(),
                pomodoroRange = dataProvider.getRangeModel(POMODORO_RANGE)
                    ?: TimerDefaults.pomodoro(),
                percentage = TimerDefaults.percentage(dataProvider.getLong(PERCENTAGE_RANGE))
            )
        }
    }

    fun addRange() {
        val rangesList = _uiState.value.flowTimeRanges.toMutableList()
        rangesList.add(
            index = rangesList.size - 1,
            element = RangeModel(totalRange = 0, endRange = 15, rest = 15)
        )
        _uiState.update { it.copy(flowTimeRanges = rangesList.withCumulativeTotals()) }
    }

    fun modifyRange(index: Int, range: RangeModel) {
        val rangesList = _uiState.value.flowTimeRanges.toMutableList()
        rangesList[index] = range
        _uiState.update { it.copy(flowTimeRanges = rangesList.withCumulativeTotals()) }
    }

    fun deleteRange(index: Int) {
        val rangesList = _uiState.value.flowTimeRanges.toMutableList()
        rangesList.removeAt(index)
        _uiState.update { it.copy(flowTimeRanges = rangesList.withCumulativeTotals()) }
    }

    fun modifyPomodoro(range: RangeModel) {
        _uiState.update { it.copy(pomodoroRange = range) }
    }

    fun modifyPercentage(percentage: Long) {
        _uiState.update { it.copy(percentage = percentage) }
    }

    // Solo el modo que se está editando: guardar los tres pisaba la configuración de los otros dos.
    fun saveChanges() {
        with(_uiState.value) {
            when (screenType) {
                ScreenType.FlowTime ->
                    dataProvider.setObject(FLOW_TIME_RANGE, flowTimeRanges.toMutableList())

                ScreenType.Pomodoro -> dataProvider.setObject(POMODORO_RANGE, pomodoroRange)
                ScreenType.Percentage -> dataProvider.setLong(PERCENTAGE_RANGE, percentage)
            }
        }
    }
}