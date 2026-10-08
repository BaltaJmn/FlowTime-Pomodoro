package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.data.repository.FocusSession
import com.baltajmn.flowtime.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Sesiones en memoria, con los totales por día en segundos. Los días van en UTC. */
class FakeSessions : SessionRepository {

    data class Recorded(
        val mode: String,
        val startedAt: Long,
        val endedAt: Long,
        val focusSeconds: Long,
        val tagId: Long? = null,
        val taskId: Long? = null
    )

    val recorded = mutableListOf<Recorded>()
    val days = mutableMapOf<LocalDate, Long>()

    override fun record(
        mode: String,
        startedAt: Long,
        endedAt: Long,
        focusSeconds: Long,
        tagId: Long?,
        taskId: Long?
    ) {
        recorded += Recorded(mode, startedAt, endedAt, focusSeconds, tagId, taskId)
        val day = Instant.fromEpochMilliseconds(startedAt).toLocalDateTime(TimeZone.UTC).date
        days[day] = (days[day] ?: 0L) + focusSeconds
    }

    override fun secondsOn(day: LocalDate): Flow<Long> = flowOf(days[day] ?: 0L)

    override suspend fun secondsByDay(from: LocalDate, to: LocalDate) =
        days.filterKeys { it in from..to }

    override suspend fun totalSeconds() = days.values.sum()

    override suspend fun countSessions(from: LocalDate, to: LocalDate) = recorded.count {
        val day = Instant.fromEpochMilliseconds(it.startedAt).toLocalDateTime(TimeZone.UTC).date
        day in from..to
    }

    override suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean) {
        secondsByDay.forEach { (day, seconds) ->
            val base = if (replace) 0L else days[day] ?: 0L
            days[day] = (base + seconds).coerceAtMost(24 * 60 * 60L)
        }
    }

    override fun importLegacyOnce() = Unit

    val unrecorded = mutableListOf<Long>()

    override fun unrecord(startedAt: Long) {
        unrecorded += startedAt
    }

    val sessions = kotlinx.coroutines.flow.MutableStateFlow<List<FocusSession>>(emptyList())

    override fun sessionsOn(day: LocalDate): Flow<List<FocusSession>> = sessions

    override suspend fun delete(id: Long) = sessions.update { list -> list.filterNot { it.id == id } }

    override suspend fun restore(session: FocusSession) = sessions.update { list -> (list + session).sortedBy { it.startedAt } }

    override suspend fun setSeconds(id: Long, seconds: Long) = sessions.update { list ->
        list.map { if (it.id == id) it.copy(focusSeconds = seconds) else it }
    }
}
