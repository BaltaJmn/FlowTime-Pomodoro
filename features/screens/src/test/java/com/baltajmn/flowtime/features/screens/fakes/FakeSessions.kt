package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.data.repository.SessionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Sesiones en memoria, con los totales por día en segundos. Los días van en UTC. */
class FakeSessions : SessionRepository {

    data class Recorded(
        val mode: String,
        val startedAt: Long,
        val endedAt: Long,
        val focusSeconds: Long,
        val tagId: Long? = null
    )

    val recorded = mutableListOf<Recorded>()
    val days = mutableMapOf<LocalDate, Long>()

    override fun record(
        mode: String,
        startedAt: Long,
        endedAt: Long,
        focusSeconds: Long,
        tagId: Long?
    ) {
        recorded += Recorded(mode, startedAt, endedAt, focusSeconds, tagId)
        val day = Instant.ofEpochMilli(startedAt).atZone(ZoneOffset.UTC).toLocalDate()
        days[day] = (days[day] ?: 0L) + focusSeconds
    }

    override fun secondsOn(day: LocalDate): Flow<Long> = flowOf(days[day] ?: 0L)

    override suspend fun secondsByDay(from: LocalDate, to: LocalDate) =
        days.filterKeys { !it.isBefore(from) && !it.isAfter(to) }

    override suspend fun totalSeconds() = days.values.sum()

    override suspend fun countSessions(from: LocalDate, to: LocalDate) = recorded.count {
        val day = Instant.ofEpochMilli(it.startedAt).atZone(ZoneOffset.UTC).toLocalDate()
        !day.isBefore(from) && !day.isAfter(to)
    }

    override suspend fun addToDays(secondsByDay: Map<LocalDate, Long>, replace: Boolean) {
        secondsByDay.forEach { (day, seconds) ->
            val base = if (replace) 0L else days[day] ?: 0L
            days[day] = (base + seconds).coerceAtMost(24 * 60 * 60L)
        }
    }

    override fun importLegacyOnce() = Unit
}
