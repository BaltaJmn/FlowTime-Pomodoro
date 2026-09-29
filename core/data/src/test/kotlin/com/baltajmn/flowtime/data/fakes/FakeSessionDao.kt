package com.baltajmn.flowtime.data.fakes

import com.baltajmn.flowtime.core.database.datasource.SessionDao
import com.baltajmn.flowtime.core.database.model.DaySeconds
import com.baltajmn.flowtime.core.database.model.HourSeconds
import com.baltajmn.flowtime.core.database.model.ModeSeconds
import com.baltajmn.flowtime.core.database.model.PeriodTotals
import com.baltajmn.flowtime.core.database.model.SessionDb
import com.baltajmn.flowtime.core.database.model.TagSeconds
import com.baltajmn.flowtime.core.database.model.TaskSeconds
import com.baltajmn.flowtime.core.database.model.TaskTotal
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * La tabla de sesiones en memoria, con las mismas consultas que la de verdad: lo justo para que
 * funcionen las transacciones de la clase base. Las horas van en UTC.
 */
class FakeSessionDao : SessionDao() {
    val rows = mutableListOf<SessionDb>()
    private var nextId = 1L

    override suspend fun insert(session: SessionDb) {
        rows += session.copy(id = nextId++)
    }

    override suspend fun secondsByDay(from: String, to: String) = rows
        .filter { it.localDate in from..to }
        .groupBy { it.localDate }
        .map { (day, sessions) -> DaySeconds(day, sessions.sumOf { it.focusSeconds }) }

    override fun secondsOn(day: String): Flow<Long> = flowOf(secondsOf(day))

    override suspend fun totalSeconds() = rows.sumOf { it.focusSeconds }

    override suspend fun countSessions(from: String, to: String) = sessions(from, to).size

    override suspend fun totals(from: String, to: String): PeriodTotals {
        val sessions = sessions(from, to)
        return PeriodTotals(
            totalSeconds = rows.filter { it.localDate in from..to }.sumOf { it.focusSeconds },
            sessions = sessions.size,
            sessionSeconds = sessions.sumOf { it.focusSeconds }
        )
    }

    override suspend fun secondsByHour(from: String, to: String) = sessions(from, to)
        .groupBy { Instant.ofEpochMilli(it.startedAt).atZone(ZoneOffset.UTC).hour }
        .map { (hour, sessions) -> HourSeconds(hour, sessions.sumOf { it.focusSeconds }) }

    override suspend fun secondsByMode(from: String, to: String) = sessions(from, to)
        .groupBy { it.mode }
        .map { (mode, sessions) -> ModeSeconds(mode, sessions.sumOf { it.focusSeconds }, sessions.size) }

    override suspend fun secondsByTag(from: String, to: String) = sessions(from, to)
        .groupBy { it.tagId }
        .map { (tag, sessions) -> TagSeconds(tag, sessions.sumOf { it.focusSeconds }) }

    override suspend fun all() = rows.sortedBy { it.startedAt }

    override fun secondsByTask(): Flow<List<TaskSeconds>> = flowOf(
        rows.filter { it.taskId != null }
            .groupBy { it.taskId!! }
            .map { (task, sessions) -> TaskSeconds(task, sessions.sumOf { it.focusSeconds }) }
    )

    /** Sin la tabla de tareas: el título es el id. */
    override suspend fun topTasks(from: String, to: String) = rows
        .filter { it.taskId != null && it.localDate in from..to }
        .groupBy { it.taskId!! }
        .map { (task, sessions) -> TaskTotal(task, "$task", sessions.sumOf { it.focusSeconds }) }
        .sortedByDescending { it.seconds }

    override suspend fun secondsOnce(day: String) = secondsOf(day)

    override suspend fun deleteDay(day: String) {
        rows.removeAll { it.localDate == day }
    }

    override suspend fun deleteLegacyDay(day: String) {
        rows.removeAll { it.localDate == day && it.mode == SessionDb.MODE_LEGACY }
    }

    private fun sessions(from: String, to: String) =
        rows.filter { it.mode != SessionDb.MODE_LEGACY && it.localDate in from..to }

    private fun secondsOf(day: String) = rows.filter { it.localDate == day }.sumOf { it.focusSeconds }
}
