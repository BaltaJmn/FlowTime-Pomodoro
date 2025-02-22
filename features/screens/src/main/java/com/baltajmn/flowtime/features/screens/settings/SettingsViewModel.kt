package com.baltajmn.flowtime.features.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PERCENTAGE_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.THEME_COLOR
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val dataProvider: DataProvider,
    private val getAllStudyTimeUseCase: GetAllStudyTimeUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState

    init {
        getConfig()
        getUserLevel()
    }

    private fun getConfig() {
        val flowTimeList =
            dataProvider.getRangeModelList(key = FLOW_TIME_RANGE)
                ?: _uiState.value.flowTimeRanges
        val pomodoroList =
            dataProvider.getRangeModel(key = POMODORO_RANGE)
                ?: _uiState.value.pomodoroRange
        val percentage =
            dataProvider.getLong(key = PERCENTAGE_RANGE)
        _uiState.update {
            it.copy(
                flowTimeRanges = flowTimeList,
                pomodoroRange = pomodoroList,
                percentage = percentage
            )
        }
    }

    private fun getUserLevel() {
        viewModelScope.launch {
            val userLevel = getAllStudyTimeUseCase()
            val xpBase = 100
            val xpPerHour = 50
            val xpTotal = (userLevel * xpPerHour).toInt()

            var level = 0
            var xpRequiredForCurrentLevel = 0
            var xpRequiredForNextLevel = xpBase

            while (xpTotal >= xpRequiredForNextLevel) {
                level++
                xpRequiredForCurrentLevel = xpRequiredForNextLevel
                xpRequiredForNextLevel = xpBase * (level + 1) * (level + 1)
            }

            val xpInCurrentLevel = xpTotal - xpRequiredForCurrentLevel
            val xpNeededInThisLevel = xpRequiredForNextLevel - xpRequiredForCurrentLevel
            val progressPercentage = (xpInCurrentLevel.toDouble() / xpNeededInThisLevel) * 100

            _uiState.update {
                it.copy(
                    userLevel = level.toLong(),
                    progressPercentage = progressPercentage.toLong()
                )
            }
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

    fun saveColor(color: AppTheme) {
        dataProvider.setString(THEME_COLOR, color.name)
    }

    fun saveSound(showSound: Boolean) {
        dataProvider.setBoolean(SHOW_SOUND, showSound)
    }
}