package com.baltajmn.flowtime.features.screens.edit

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PERCENTAGE_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.getObject
import com.baltajmn.flowtime.core.persistence.sharedpreferences.setObject
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.data.timer.withCumulativeTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Los ajustes de un modo, en la hoja que se abre desde la pantalla de concentración. */
class EditViewModel(
    private val mode: TimerMode,
    private val dataProvider: DataProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditState(mode = mode))
    val uiState: StateFlow<EditState> = _uiState

    init {
        reload()
    }

    /** Lo guardado: al abrir la hoja otra vez, lo que se cambió sin guardar no sigue ahí. */
    fun reload() {
        _uiState.update {
            it.copy(
                flowTimeRanges = (
                    dataProvider.getObject<List<RangeModel>>(FLOW_TIME_RANGE)
                        ?: TimerDefaults.flowTimeRanges()
                    ).withCumulativeTotals(),
                pomodoroRange = dataProvider.getObject<RangeModel>(POMODORO_RANGE)
                    ?: TimerDefaults.pomodoro(),
                percentage = TimerDefaults.percentage(dataProvider.getLong(PERCENTAGE_RANGE)),
                continueAfterBreak = dataProvider.getCheckValue(mode.continueAfterBreakKey)
            )
        }
    }

    /** Se aplica al momento, como antes en el temporizador. */
    fun setContinueAfterBreak(value: Boolean) {
        dataProvider.setCheckValue(mode.continueAfterBreakKey, value)
        _uiState.update { it.copy(continueAfterBreak = value) }
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
            when (mode) {
                TimerMode.FLOW_TIME -> dataProvider.setObject(
                    FLOW_TIME_RANGE,
                    flowTimeRanges.toMutableList()
                )
                TimerMode.POMODORO -> dataProvider.setObject(POMODORO_RANGE, pomodoroRange)
                TimerMode.PERCENTAGE -> dataProvider.setLong(PERCENTAGE_RANGE, percentage)
            }
        }
    }
}