package com.baltajmn.flowtime.features.screens.stats

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.pro.ProFeatures
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.stats.Level
import com.baltajmn.flowtime.data.stats.PeriodKind
import com.baltajmn.flowtime.data.stats.StatsPeriod
import com.baltajmn.flowtime.data.stats.StatsRepository
import com.baltajmn.flowtime.data.stats.StatsSummary
import com.baltajmn.flowtime.data.stats.TaskTime
import com.baltajmn.flowtime.data.stats.level
import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboardUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.ImportMode
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboardUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.StudyTimeImport
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Las estadísticas (#38). Gratis: hoy, la semana y el mes, la racha, el objetivo y el nivel. De
 * Pro: el año, por hora, por modo, por etiqueta, por tarea, la comparación y el CSV. Sin Pro a la
 * venta ([ProFeatures.enabled]) lo de Pro ni se consulta.
 */
class StatsViewModel(
    private val stats: StatsRepository,
    private val goals: GoalRepository,
    private val tags: TagRepository,
    private val purchases: PurchasesRepository,
    private val files: DocumentFiles,
    private val getAllStudyTime: GetAllStudyTimeUseCase,
    private val getStudyTimeToClipboard: GetStudyTimeToClipboardUseCase,
    private val setStudyTimeFromClipboard: SetStudyTimeFromClipboardUseCase,
    private val proEnabled: Boolean = ProFeatures.enabled,
    private val today: () -> LocalDate = { LocalDate.now() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    private var loading: Job? = null

    init {
        viewModelScope.launch {
            combine(goals.today, goals.streak, ::Pair).collect { (day, streak) ->
                _uiState.update { it.copy(today = day, streak = streak) }
            }
        }
        viewModelScope.launch {
            tags.all.collect { all -> _uiState.update { it.copy(tags = all) } }
        }
        viewModelScope.launch {
            purchases.isPro.collect { pro ->
                val access = ProAccess.of(pro, proEnabled)
                if (access == _uiState.value.pro) return@collect
                _uiState.update { it.copy(pro = access) }
                // Recién comprado, o sin Pro después de un reembolso: cambia lo que hay que consultar.
                load()
            }
        }
    }

    /** Al entrar en la pantalla: puede haber sesiones nuevas desde la última vez. */
    fun load() = load(_uiState.value.period)

    fun selectPeriod(kind: PeriodKind) = load(StatsPeriod(kind))

    fun previousPeriod() = load(_uiState.value.period.previous)

    /** Hasta el periodo en curso, no más allá. */
    fun nextPeriod() {
        val period = _uiState.value.period
        if (period.offset > 0) load(period.copy(offset = period.offset - 1))
    }

    // Con las flechas pulsadas deprisa, solo cuenta la última: una consulta lenta no pisa a otra.
    private fun load(period: StatsPeriod) {
        loading?.cancel()
        loading = viewModelScope.launch {
            val now = today()
            val totalMinutes = getAllStudyTime()
            val summary = stats.summary(period, now)
            val details = if (_uiState.value.pro == ProAccess.HIDDEN || summary.totalSeconds == 0L) {
                StatsDetails()
            } else {
                details(period, now)
            }
            _uiState.update {
                it.copy(
                    loading = false,
                    hasSessions = totalMinutes > 0,
                    level = level(totalMinutes),
                    period = period,
                    range = period.range(now),
                    summary = summary,
                    details = details
                )
            }
        }
    }

    private suspend fun details(period: StatsPeriod, now: LocalDate) = StatsDetails(
        change = stats.change(period, now),
        byHour = stats.byHour(period, now),
        byMode = stats.byMode(period, now).mapNotNull { (name, seconds) ->
            TimerMode.entries.firstOrNull { it.name == name }?.let { it to seconds }
        }.sortedByDescending { it.second },
        byTag = stats.byTag(period, now).toList().sortedByDescending { it.second },
        topTasks = stats.topTasks(period, now).take(TOP_TASKS)
    )

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            val names = tags.all.value.associate { it.id to it.name }
            val saved = runCatching { files.write(uri, stats.csv(names)) }.isSuccess
            _uiState.update {
                it.copy(
                    message = if (saved) StatsMessage.CsvSaved else StatsMessage.CsvFailed
                )
            }
        }
    }

    /** El historial en texto, para pegarlo en otro móvil con una versión antigua. */
    fun exportStudyTime(onText: (String) -> Unit) {
        viewModelScope.launch { onText(getStudyTimeToClipboard()) }
    }

    fun importStudyTime(data: String) {
        viewModelScope.launch {
            val studyTime = setStudyTimeFromClipboard.preview(data)
            when {
                studyTime.minutesByDay.isEmpty() -> _uiState.update {
                    it.copy(
                        message = StatsMessage.Imported(
                            days = 0,
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

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private suspend fun applyImport(studyTime: StudyTimeImport, mode: ImportMode) {
        setStudyTimeFromClipboard.apply(studyTime, mode)
        _uiState.update {
            it.copy(
                message = StatsMessage.Imported(
                    days = studyTime.minutesByDay.size,
                    ignoredLines = studyTime.ignoredLines
                )
            )
        }
        load()
    }

    private companion object {
        const val TOP_TASKS = 5
    }
}

/** Lo de Pro: solo se consulta si Pro está a la venta, y con Pro o sin él (para verse difuminado). */
data class StatsDetails(
    /** Frente al periodo anterior, en tanto por uno; null si el anterior no tenía nada. */
    val change: Float? = null,
    val byHour: List<Long> = List(24) { 0L },
    val byMode: List<Pair<TimerMode, Long>> = emptyList(),
    /** La etiqueta null es "Sin etiqueta". */
    val byTag: List<Pair<Long?, Long>> = emptyList(),
    val topTasks: List<TaskTime> = emptyList()
)

sealed interface StatsMessage {
    data class Imported(val days: Int, val ignoredLines: Int) : StatsMessage
    data object CsvSaved : StatsMessage
    data object CsvFailed : StatsMessage
}

data class StatsUiState(
    /** Hasta la primera consulta: sin ella, no se sabe si enseñar el estado vacío. */
    val loading: Boolean = true,
    val hasSessions: Boolean = false,
    val level: Level = Level(0, 0),
    val today: DayProgress? = null,
    val streak: Streak = Streak(),
    val period: StatsPeriod = StatsPeriod(PeriodKind.WEEK),
    val range: ClosedRange<LocalDate> = StatsPeriod(PeriodKind.WEEK).range(LocalDate.now()),
    val summary: StatsSummary = StatsSummary(),
    val pro: ProAccess = ProAccess.HIDDEN,
    val details: StatsDetails = StatsDetails(),
    /** Todas, también las archivadas: el tiempo de antes sigue siendo suyo. */
    val tags: List<Tag> = emptyList(),
    val pendingImport: StudyTimeImport? = null,
    val message: StatsMessage? = null
)
