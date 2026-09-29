package com.baltajmn.flowtime.features.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.common.extensions.formatAllStudyTime
import com.baltajmn.flowtime.core.common.extensions.toShowInSelector
import com.baltajmn.flowtime.data.stats.PeriodKind
import com.baltajmn.flowtime.data.stats.StatsPeriod
import com.baltajmn.flowtime.data.stats.StatsRepository
import com.baltajmn.flowtime.data.stats.StatsSummary
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboardUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.ImportMode
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboardUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.StudyTimeImport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class HistoryViewModel(
    private val getStudyTime: GetStudyTimeUseCase,
    private val getAllStudyTimeUseCase: GetAllStudyTimeUseCase,
    private val getStudyTimeToClipboard: GetStudyTimeToClipboardUseCase,
    private val setStudyTimeFromClipboard: SetStudyTimeFromClipboardUseCase,
    private val stats: StatsRepository,
    private val today: () -> LocalDate = { LocalDate.now() }
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
        loadSummary(_uiState.value.period)
    }

    /** Hoy, esta semana o este mes. */
    fun selectPeriod(kind: PeriodKind) = loadSummary(StatsPeriod(kind))

    fun previousPeriod() = loadSummary(_uiState.value.period.previous)

    /** Hasta el periodo en curso, no más allá. */
    fun nextPeriod() {
        val period = _uiState.value.period
        if (period.offset > 0) loadSummary(period.copy(offset = period.offset - 1))
    }

    private fun loadSummary(period: StatsPeriod) {
        viewModelScope.launch {
            val now = today()
            val summary = stats.summary(period, now)
            _uiState.update {
                it.copy(period = period, periodRange = period.range(now), summary = summary)
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
            val studyTime = setStudyTimeFromClipboard.preview(data)
            when {
                studyTime.minutesByDay.isEmpty() -> _uiState.update {
                    it.copy(
                        importSummary = ImportSummary(
                            importedDays = 0,
                            ignoredLines = studyTime.ignoredLines
                        )
                    )
                }

                // Hay días con datos: se pregunta si sustituirlos o sumarlos.
                studyTime.daysWithData > 0 -> _uiState.update { it.copy(pendingImport = studyTime) }
                else -> applyImport(studyTime, ImportMode.REPLACE)
            }
        }
    }

    /** Respuesta al diálogo de días con datos; null es cancelar. */
    fun resolvePendingImport(mode: ImportMode?) {
        val studyTime = _uiState.value.pendingImport ?: return
        _uiState.update { it.copy(pendingImport = null) }
        if (mode != null) viewModelScope.launch { applyImport(studyTime, mode) }
    }

    fun onImportSummaryShown() {
        _uiState.update { it.copy(importSummary = null) }
    }

    private suspend fun applyImport(studyTime: StudyTimeImport, mode: ImportMode) {
        setStudyTimeFromClipboard.apply(studyTime, mode)

        _uiState.update {
            it.copy(
                studyTime = getStudyTime(it.selectedDate),
                allStudyTime = getAllStudyTimeUseCase().formatAllStudyTime(),
                importSummary = ImportSummary(
                    importedDays = studyTime.minutesByDay.size,
                    ignoredLines = studyTime.ignoredLines
                )
            )
        }
    }
}

data class ImportSummary(val importedDays: Int, val ignoredLines: Int)

data class HistoryState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedDateToShow: String = LocalDate.now().toShowInSelector(),
    // Siempre los siete días: el gráfico los lee por posición, y las sesiones llegan un momento después.
    val studyTime: List<Long> = List(7) { 0L },
    val allStudyTime: String = "",
    val pendingImport: StudyTimeImport? = null,
    val importSummary: ImportSummary? = null,
    /** El resumen de #38: lo gratis, hoy, la semana y el mes. */
    val period: StatsPeriod = StatsPeriod(PeriodKind.WEEK),
    val periodRange: ClosedRange<LocalDate> = StatsPeriod(PeriodKind.WEEK).range(LocalDate.now()),
    val summary: StatsSummary = StatsSummary()
)