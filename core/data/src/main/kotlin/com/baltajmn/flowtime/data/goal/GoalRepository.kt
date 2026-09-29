package com.baltajmn.flowtime.data.goal

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DAILY_GOAL
import com.baltajmn.flowtime.data.repository.SessionRepository
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** Lo que se lleva hoy frente al objetivo de hoy. */
data class DayProgress(val day: LocalDate, val seconds: Long, val goalMinutes: Int) {
    val met get() = seconds >= goalMinutes * 60L

    /** De 0 a 1. */
    val fraction get() = (seconds / (goalMinutes * 60f)).coerceIn(0f, 1f)
}

/**
 * El objetivo diario y la racha. El objetivo guarda su historial: cambiarlo vale desde hoy, y cada
 * día pasado se sigue midiendo con el que tenía entonces.
 */
class GoalRepository(
    private val dataProvider: DataProvider,
    private val sessions: SessionRepository,
    private val days: Flow<LocalDate> = currentDay()
) {
    private val _history = MutableStateFlow(read())
    val history: StateFlow<List<GoalChange>> = _history.asStateFlow()

    /** El objetivo de hoy en adelante. */
    val goalMinutes: Flow<Int> = _history.map { it.current() }

    val currentGoal: Int get() = _history.value.current()

    // Sin distinctUntilChanged: Room vuelve a emitir con cualquier sesión nueva, también si es de
    // otro día (una importación), y la racha tiene que recalcularse entonces.
    private val progress: Flow<DayProgress> = days.flatMapLatest { day ->
        combine(sessions.secondsOn(day), _history) { seconds, history ->
            DayProgress(day, seconds, goalOn(day, history))
        }
    }

    val today: Flow<DayProgress> = progress.distinctUntilChanged()

    val streak: Flow<Streak> = progress
        .map { streak(sessions.secondsByDay(FIRST_DAY, it.day), _history.value, it.day) }
        .distinctUntilChanged()

    /** De 5 en 5, entre 10 y 480 minutos. Sustituye al cambio de hoy, si ya lo había. */
    fun setGoal(minutes: Int, today: LocalDate = LocalDate.now()) {
        val goal = (minutes / DailyGoal.STEP_MINUTES * DailyGoal.STEP_MINUTES)
            .coerceIn(DailyGoal.MIN_MINUTES, DailyGoal.MAX_MINUTES)
        val before = _history.value.filter { it.from.isBefore(today) }
        val history = if (goalOn(today, before) == goal) before else before + GoalChange(today, goal)
        save(history)
    }

    /** Todo el historial de golpe: lo usa la copia de seguridad. Lo que no tiene sentido se salta. */
    fun restore(history: List<GoalChange>) = save(
        history
            .filter { it.minutes in DailyGoal.MIN_MINUTES..DailyGoal.MAX_MINUTES }
            .sortedBy { it.from }
            .distinctBy { it.from }
    )

    private fun save(history: List<GoalChange>) {
        val stored = history.map { StoredChange(it.from.toString(), it.minutes) }
        dataProvider.setString(DAILY_GOAL, json.encodeToString(serializer, stored))
        _history.value = history
    }

    private fun read(): List<GoalChange> {
        val text = dataProvider.getString(DAILY_GOAL) ?: return emptyList()
        val stored = try {
            json.decodeFromString(serializer, text)
        } catch (e: IllegalArgumentException) {
            return emptyList()
        }
        return stored.mapNotNull { change ->
            try {
                GoalChange(LocalDate.parse(change.from), change.minutes)
            } catch (e: DateTimeParseException) {
                null
            }
        }.sortedBy { it.from }
    }

    private fun List<GoalChange>.current() = lastOrNull()?.minutes ?: DailyGoal.DEFAULT_MINUTES

    @Serializable
    private data class StoredChange(val from: String, val minutes: Int)

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
        val serializer = ListSerializer(StoredChange.serializer())
        val FIRST_DAY: LocalDate = LocalDate.of(1970, 1, 1)
    }
}

/**
 * El día de hoy, que cambia a medianoche. Se mira cada minuto y no se espera hasta la medianoche:
 * con el móvil dormido, las esperas largas se alargan todo lo que duerma.
 */
fun currentDay(zone: () -> ZoneId = { ZoneId.systemDefault() }): Flow<LocalDate> = flow {
    while (true) {
        emit(LocalDate.now(zone()))
        delay(60_000)
    }
}.distinctUntilChanged()
