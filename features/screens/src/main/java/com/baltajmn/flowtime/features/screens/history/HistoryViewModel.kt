package com.baltajmn.flowtime.features.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.common.extensions.formatAllStudyTime
import com.baltajmn.flowtime.core.common.extensions.toShowInSelector
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboardUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboardUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class HistoryViewModel(
    private val getStudyTime: GetStudyTimeUseCase,
    private val getAllStudyTimeUseCase: GetAllStudyTimeUseCase,
    private val getStudyTimeToClipboard: GetStudyTimeToClipboardUseCase,
    private val setStudyTimeFromClipboard: SetStudyTimeFromClipboardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryState())
    val uiState: StateFlow<HistoryState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    studyTime = getStudyTime(_uiState.value.selectedDate),
                    allStudyTime = getAllStudyTimeUseCase().formatAllStudyTime()
                )
            }
        }
    }

    fun plusWeek() {
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate.plusWeeks(1)
            _uiState.update {
                it.copy(
                    selectedDate = selectedDate,
                    selectedDateToShow = selectedDate.toShowInSelector(),
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
                    selectedDateToShow = selectedDate.toShowInSelector(),
                    studyTime = getStudyTime(selectedDate)
                )
            }
        }
    }

    fun exportStudyTime(onStringGenerated: (String) -> Unit) {
        viewModelScope.launch {
            onStringGenerated.invoke(getStudyTimeToClipboard())
        }
    }

    fun importStudyTime(data: String) {
        viewModelScope.launch {
            setStudyTimeFromClipboard(data)

            _uiState.update {
                it.copy(
                    studyTime = getStudyTime(_uiState.value.selectedDate)
                )
            }
        }
    }
}

data class HistoryState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateToShow: String = LocalDate.now().toShowInSelector(),
    val studyTime: List<Long> = listOf(),
    val allStudyTime: String = ""
)