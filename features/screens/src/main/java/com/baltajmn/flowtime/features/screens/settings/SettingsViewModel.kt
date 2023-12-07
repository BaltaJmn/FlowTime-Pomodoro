package com.baltajmn.flowtime.features.screens.settings

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(
    dispatcherProvider: DispatcherProvider,
    private val dataProvider: DataProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState

    init {
        getRanges()
    }

    private fun getRanges() {
        val flowTimeList =
            dataProvider.getRangeModelList(key = FLOW_TIME_RANGE)
                ?: mutableListOf(
                    RangeModel(totalRange = 15, endRange = 15, rest = 5),
                    RangeModel(totalRange = 30, endRange = 15, rest = 10),
                    RangeModel(totalRange = 15, endRange = 15, rest = 15)
                )

        val pomodoroList =
            dataProvider.getObject(key = POMODORO_RANGE)
                ?: RangeModel(totalRange = 45, endRange = 45, rest = 15)

        _uiState.update {
            it.copy(
                flowTimeRanges = flowTimeList,
                pomodoroRange = pomodoroList
            )
        }
    }

    fun addRange() {
        with(_uiState.value) {
            val rangesList = flowTimeRanges.toMutableList()
            rangesList.add(
                index = rangesList.size - 1,
                element = RangeModel(
                    totalRange = rangesList.dropLast(1).last().totalRange + 15,
                    endRange = 15,
                    rest = 15
                )
            )
            rangesList.last().totalRange = rangesList.dropLast(1).sumOf { it.endRange }
            _uiState.update {
                it.copy(flowTimeRanges = rangesList.toMutableList())
            }
        }
    }

    fun modifyRange(index: Int, range: RangeModel) {
        with(_uiState.value) {
            val rangesList = flowTimeRanges.toMutableList()

            if (index > 0) {
                for (i in index..<rangesList.size) {
                    rangesList[i] = rangesList.toMutableList()[i].copy(
                        totalRange = if (i == index) {
                            rangesList[i - 1].totalRange + range.endRange
                        } else {
                            rangesList[i - 1].totalRange + rangesList[i].endRange
                        },
                        endRange = if (i == index) {
                            range.endRange
                        } else {
                            rangesList[i].endRange
                        },
                        rest = if (i == index) {
                            range.rest
                        } else {
                            rangesList[i].rest
                        },
                    )
                }
            } else {
                rangesList[index] = rangesList.toMutableList()[index].copy(
                    totalRange = range.totalRange,
                    endRange = range.endRange,
                    rest = range.rest
                )
            }

            _uiState.update {
                it.copy(flowTimeRanges = rangesList.toMutableList())
            }
        }
    }

    fun deleteRange(index: Int) {
        with(_uiState.value) {
            val rangesList = flowTimeRanges.toMutableList()
            rangesList.removeAt(index)
            _uiState.update {
                it.copy(flowTimeRanges = rangesList.toMutableList())
            }
        }
    }

    fun modifyPomodoro(range: RangeModel) {
        _uiState.update { it.copy(pomodoroRange = range) }
    }

    fun saveChanges() {
        dataProvider.setObject(FLOW_TIME_RANGE, _uiState.value.flowTimeRanges.toMutableList())
        dataProvider.setObject(POMODORO_RANGE, _uiState.value.pomodoroRange)
    }

    fun changeColor(){
        _uiState.update { it.copy(theme = AppTheme.Brown) }
    }
}