package com.baltajmn.flowtime.data.stats

import com.baltajmn.flowtime.core.database.datasource.SessionDao
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class PeriodKind { DAY, WEEK, MONTH, YEAR }

/** Un día, una semana (de lunes a domingo), un mes o un año; [offset] periodos hacia atrás. */
data class StatsPeriod(val kind: PeriodKind, val offset: Int = 0) {

    fun range(today: LocalDate): ClosedRange<LocalDate> {
        val start = when (kind) {
            PeriodKind.DAY -> today.minusDays(offset.toLong())
            PeriodKind.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(offset.toLong())
            PeriodKind.MONTH -> today.withDayOfMonth(1).minusMonths(offset.toLong())
            PeriodKind.YEAR -> today.withDayOfYear(1).minusYears(offset.toLong())
        }
        val end = when (kind) {
            PeriodKind.DAY -> start
            PeriodKind.WEEK -> start.plusDays(6)
            PeriodKind.MONTH -> start.plusMonths(1).minusDays(1)
            PeriodKind.YEAR -> start.plusYears(1).minusDays(1)
        }
        return start..end
    }

    val previous: StatsPeriod get() = copy(offset = offset + 1)
}

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

    /** Todas las sesiones en CSV, con el nombre de su etiqueta. Es de Pro (#38). */
    suspend fun csv(tagNames: Map<Long, String>): String
}

class DefaultStatsRepository(
    private val dao: SessionDao,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() }
) : StatsRepository {

    override suspend fun summary(period: StatsPeriod, today: LocalDate): StatsSummary {
        val range = period.range(today)
        val from = range.start.toString()
        val to = range.endInclusive.toString()
        val totals = dao.totals(from, to)
        val byDay = dao.secondsByDay(from, to).associate { LocalDate.parse(it.localDate) to it.seconds }
        val best = byDay.maxByOrNull { it.value }?.takeIf { it.value > 0 }
        val days = ChronoUnit.DAYS.between(range.start, minOf(range.endInclusive, today)) + 1
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

    override suspend fun csv(tagNames: Map<Long, String>): String = buildString {
        appendLine("startedAt,endedAt,date,mode,tag,task,focusMinutes")
        dao.all().forEach { session ->
            appendLine(
                listOf(
                    session.startedAt.asDateTime(),
                    session.endedAt.asDateTime(),
                    session.localDate,
                    session.mode,
                    session.tagId?.let(tagNames::get).orEmpty(),
                    "",
                    String.format(Locale.ROOT, "%.1f", session.focusSeconds / 60.0)
                ).joinToString(",") { it.csvField() }
            )
        }
    }

    private suspend fun total(period: StatsPeriod, today: LocalDate): Long {
        val range = period.range(today)
        return dao.totals(range.start.toString(), range.endInclusive.toString()).totalSeconds
    }

    private fun Long.asDateTime(): String = DATE_TIME.format(Instant.ofEpochMilli(this).atZone(zone()))

    private fun String.csvField() =
        if (any { it == ',' || it == '"' || it == '\n' }) "\"" + replace("\"", "\"\"") + "\"" else this

    private companion object {
        val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT)
    }
}
