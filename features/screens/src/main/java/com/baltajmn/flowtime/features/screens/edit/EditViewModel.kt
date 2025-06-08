package com.baltajmn.flowtime.features.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PERCENTAGE_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
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
        _uiState.update { it.copy(screenType = type) }
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
            for (i in index until rangesList.size) {
                if (i > 0) {
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
                        }
                    )
                } else {
                    rangesList[index] = rangesList.toMutableList()[index].copy(
                        totalRange = range.totalRange,
                        endRange = range.endRange,
                        rest = range.rest
                    )
                }
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

    fun modifyPercentage(percentage: Long) {
        _uiState.update { it.copy(percentage = percentage) }
    }

    fun saveChanges() {
        dataProvider.setObject(FLOW_TIME_RANGE, _uiState.value.flowTimeRanges.toMutableList())
        dataProvider.setObject(POMODORO_RANGE, _uiState.value.pomodoroRange)
        dataProvider.setLong(PERCENTAGE_RANGE, _uiState.value.percentage)
    }
}