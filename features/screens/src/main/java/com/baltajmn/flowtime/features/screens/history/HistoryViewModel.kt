package com.baltajmn.flowtime.features.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate


class HistoryViewModel(
    private val getStudyTime: GetStudyTimeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryState())
    val uiState: StateFlow<HistoryState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(studyTime = getStudyTime(_uiState.value.selectedDate))
            }
        }
    }

    fun plusWeek() {
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate.plusWeeks(1)
            _uiState.update {
                it.copy(
                    selectedDate = selectedDate,
                    studyTime = getStudyTime(selectedDate)
                )
            }
        }
    }

    fun minusWeek() {
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate.minusWeeks(1)
            _uiState.update {
                it.copy(
                    selectedDate = selectedDate,
                    studyTime = getStudyTime(selectedDate)
                )
            }
        }
    }
}

data class HistoryState(
    val isLoading: Boolean = false,

    val selectedDate: LocalDate = LocalDate.now(),
    val studyTime: List<Long> = listOf()

)