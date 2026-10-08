package com.baltajmn.flowtime.data.fakes

import com.baltajmn.flowtime.data.repository.FocusSession
import com.baltajmn.flowtime.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

/**
 * Los segundos de cada día, en memoria. Como Room, [secondsOn] vuelve a emitir con cualquier cambio,
 * aunque sea de otro día.
 */
class FakeSessionRepository : SessionRepository {

    val days = MutableStateFlow<Map<LocalDate, Long>>(emptyMap())

    fun add(day: LocalDate, seconds: Long) = days.update { it + (day to (it[day] ?: 0L) + seconds) }

    override fun record(
        mode: String,
        startedAt: Long,
        endedAt: Long,
        focusSeconds: Long,
        tagId: Long?,
        taskId: Long?
    ) = Unit

    override fun secondsOn(day: LocalDate): Flow<Long> = days.map { it[day] ?: 0L }

    override suspend fun secondsByDay(from: LocalDate, to: LocalDate) =
        days.value.filterKeys { it in from..to }

    override suspend fun totalSeconds() = days.value.values.sum()

    /** Las sesiones de trabajo guardadas, sin fechas: las cuenta igual en cualquier periodo. */
    var sessionCount = 0

    override suspend fun countSessions(from: LocalDate, to: LocalDate) = sessionCount

    override suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean) {
        secondsByDay.forEach { (day, seconds) -> add(day, seconds) }
    }

    override fun importLegacyOnce() = Unit

    val unrecorded = mutableListOf<Long>()

    override fun unrecord(startedAt: Long) {
        unrecorded += startedAt
    }

    override fun sessionsOn(day: LocalDate): Flow<List<FocusSession>> = MutableStateFlow(emptyList())

    override suspend fun delete(id: Long) = Unit

    override suspend fun restore(session: FocusSession) = Unit

    override suspend fun setSeconds(id: Long, seconds: Long) = Unit
}
