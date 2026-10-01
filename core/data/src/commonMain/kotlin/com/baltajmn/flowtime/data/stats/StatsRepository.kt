package com.baltajmn.flowtime.data.stats

import com.baltajmn.flowtime.core.database.datasource.SessionDao
import com.baltajmn.flowtime.data.goal.weekStart
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

enum class PeriodKind { DAY, WEEK, MONTH, YEAR }

/** Un día, una semana (de lunes a domingo), un mes o un año; [offset] periodos hacia atrás. */
data class StatsPeriod(val kind: PeriodKind, val offset: Int = 0) {

    fun range(today: LocalDate): ClosedRange<LocalDate> {
        val start = when (kind) {
            PeriodKind.DAY -> today.minus(offset, DateTimeUnit.DAY)
            PeriodKind.WEEK -> today.weekStart().minus(offset, DateTimeUnit.WEEK)
            PeriodKind.MONTH -> LocalDate(today.year, today.month, 1).minus(offset, DateTimeUnit.MONTH)
            PeriodKind.YEAR -> LocalDate(today.year, 1, 1).minus(offset, DateTimeUnit.YEAR)
        }
        val end = when (kind) {
            PeriodKind.DAY -> start
            PeriodKind.WEEK -> start.plus(6, DateTimeUnit.DAY)
            PeriodKind.MONTH -> start.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
            PeriodKind.YEAR -> start.plus(1, DateTimeUnit.YEAR).minus(1, DateTimeUnit.DAY)
        }
        return start..end
    }

    val previous: StatsPeriod get() = copy(offset = offset + 1)
}

/** Una tarea con el tiempo que se le ha dedicado en un periodo. */
data class TaskTime(val taskId: Long, val title: String, val seconds: Long)

data class StatsSummary(
    val totalSeconds: Long = 0,
    /** Sin el tiempo importado (LEGACY), que no son sesiones. */
    val sessions: Int = 0,
    val averageSessionSeconds: Long = 0,
    /** Entre los días del periodo hasta hoy: los que aún no han llegado no bajan la media. */
    val dailyAverageSeconds: Long = 0,
    val bestDay: LocalDate? = null,
    val bestDaySeconds: Long = 0,
    val byDay: Map<LocalDate, Long> = emptyMap()
)

/**
 * Las estadísticas, con consultas agregadas: con un año de sesiones no hace falta cargarlas. El
 * tiempo importado (LEGACY) suma en los totales, pero no cuenta como sesión, ni por hora ni por modo.
 */
interface StatsRepository {
    suspend fun summary(period: StatsPeriod, today: LocalDate): StatsSummary

    /** Frente al periodo anterior, en tanto por uno: 0,12 es un 12 % más. null si el anterior no tenía nada. */
    suspend fun change(period: StatsPeriod, today: LocalDate): Float?

    /** 24 valores, uno por hora del día. */
    suspend fun byHour(period: StatsPeriod, today: LocalDate): List<Long>

    suspend fun byMode(period: StatsPeriod, today: LocalDate): Map<String, Long>

    /** Por etiqueta; la clave null es "Sin etiqueta". */
    suspend fun byTag(period: StatsPeriod, today: LocalDate): Map<Long?, Long>

    /** Las tareas con más tiempo del periodo (#40), la primera la que más. Es de Pro. */
    suspend fun topTasks(period: StatsPeriod, today: LocalDate): List<TaskTime>

    /** Todas las sesiones en CSV, con el nombre de su etiqueta y el título de su tarea. Es de Pro (#38). */
    suspend fun csv(tagNames: Map<Long, String>): String
}

class DefaultStatsRepository(
    private val dao: SessionDao,
    private val zone: () -> TimeZone = { TimeZone.currentSystemDefault() }
) : StatsRepository {

    override suspend fun summary(period: StatsPeriod, today: LocalDate): StatsSummary {
        val range = period.range(today)
        val from = range.start.toString()
        val to = range.endInclusive.toString()
        val totals = dao.totals(from, to)
        val byDay = dao.secondsByDay(from, to).associate { LocalDate.parse(it.localDate) to it.seconds }
        val best = byDay.maxByOrNull { it.value }?.takeIf { it.value > 0 }
        val days = range.start.daysUntil(minOf(range.endInclusive, today)) + 1
        return StatsSummary(
            totalSeconds = totals.totalSeconds,
            sessions = totals.sessions,
            averageSessionSeconds = if (totals.sessions > 0) totals.sessionSeconds / totals.sessions else 0,
            dailyAverageSeconds = if (days > 0) totals.totalSeconds / days else 0,
            bestDay = best?.key,
            bestDaySeconds = best?.value ?: 0,
            byDay = byDay
        )
    }

    override suspend fun change(period: StatsPeriod, today: LocalDate): Float? {
        val now = total(period, today)
        val before = total(period.previous, today)
        return if (before > 0) (now - before).toFloat() / before else null
    }

    override suspend fun byHour(period: StatsPeriod, today: LocalDate): List<Long> {
        val range = period.range(today)
        val hours = dao.secondsByHour(range.start.toString(), range.endInclusive.toString())
            .associate { it.hour to it.seconds }
        return (0 until 24).map { hours[it] ?: 0 }
    }

    override suspend fun byMode(period: StatsPeriod, today: LocalDate): Map<String, Long> {
        val range = period.range(today)
        return dao.secondsByMode(range.start.toString(), range.endInclusive.toString())
            .associate { it.mode to it.seconds }
    }

    override suspend fun byTag(period: StatsPeriod, today: LocalDate): Map<Long?, Long> {
        val range = period.range(today)
        return dao.secondsByTag(range.start.toString(), range.endInclusive.toString())
            .associate { it.tagId to it.seconds }
    }

    override suspend fun topTasks(period: StatsPeriod, today: LocalDate): List<TaskTime> {
        val range = period.range(today)
        return dao.topTasks(range.start.toString(), range.endInclusive.toString())
            .map { TaskTime(it.taskId, it.title, it.seconds) }
    }

    override suspend fun csv(tagNames: Map<Long, String>): String = buildString {
        // Las tareas borradas no salen: su columna queda vacía.
        val taskTitles = dao.topTasks(FIRST_DAY, LAST_DAY).associate { it.taskId to it.title }
        appendLine("startedAt,endedAt,date,mode,tag,task,focusMinutes")
        dao.all().forEach { session ->
            appendLine(
                listOf(
                    session.startedAt.asDateTime(),
                    session.endedAt.asDateTime(),
                    session.localDate,
                    session.mode,
                    session.tagId?.let(tagNames::get).orEmpty(),
                    session.taskId?.let(taskTitles::get).orEmpty(),
                    session.focusSeconds.asMinutes()
                ).joinToString(",") { it.csvField() }
            )
        }
    }

    private suspend fun total(period: StatsPeriod, today: LocalDate): Long {
        val range = period.range(today)
        return dao.totals(range.start.toString(), range.endInclusive.toString()).totalSeconds
    }

    private fun Long.asDateTime(): String = DATE_TIME.format(Instant.fromEpochMilliseconds(this).toLocalDateTime(zone()))

    /** Segundos en minutos con un decimal, redondeando como "%.1f": la mitad, hacia arriba. */
    private fun Long.asMinutes(): String {
        val tenths = (this + 3) / 6
        return "${tenths / 10}.${tenths % 10}"
    }

    private fun String.csvField() =
        if (any { it == ',' || it == '"' || it == '\n' }) "\"" + replace("\"", "\"\"") + "\"" else this

    private companion object {
        val DATE_TIME = LocalDateTime.Format {
            date(LocalDate.Formats.ISO)
            char(' ')
            hour()
            char(':')
            minute()
        }
        const val FIRST_DAY = "0000-01-01"
        const val LAST_DAY = "9999-12-31"
    }
}
