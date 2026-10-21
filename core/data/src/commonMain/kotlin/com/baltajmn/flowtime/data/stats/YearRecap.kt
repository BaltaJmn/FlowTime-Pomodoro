package com.baltajmn.flowtime.data.stats

import com.baltajmn.flowtime.data.goal.GoalChange
import com.baltajmn.flowtime.data.goal.streak
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

/**
 * El año que se resume hoy ("Tu 2026 en FlowTime"): del 1 de diciembre al 15 de enero, el que se
 * acaba o se acaba de cerrar. Fuera de esas fechas, ninguno.
 */
fun recapYear(today: LocalDate): Int? = when {
    today.month == Month.DECEMBER -> today.year
    today.month == Month.JANUARY && today.day <= RECAP_LAST_JANUARY_DAY -> today.year - 1
    else -> null
}

const val RECAP_LAST_JANUARY_DAY = 15

/** El año con los datos de este móvil, nada más: ninguno viene de un servidor. */
data class YearRecap(
    val year: Int,
    val totalSeconds: Long,
    val sessions: Int,
    /** Días del año con algo de foco. */
    val activeDays: Int,
    /** Días seguidos cumpliendo el objetivo dentro del año; 0 si no se cumplió nunca. */
    val bestStreak: Int,
    /** El día de la semana con más foco en total. */
    val bestWeekday: DayOfWeek,
    val bestDay: LocalDate,
    val bestDaySeconds: Long
)

/** null si el año no tuvo foco: no hay nada que contar, y un resumen de demostración no es el año de nadie. */
fun yearRecap(year: Int, summary: StatsSummary, goals: List<GoalChange>): YearRecap? {
    val days = summary.byDay.filterValues { it > 0 }
    val best = days.maxByOrNull { it.value } ?: return null
    val weekday = days.entries.groupBy({ it.key.dayOfWeek }, { it.value }).mapValues { it.value.sum() }
        .maxBy { it.value }.key
    return YearRecap(
        year = year,
        totalSeconds = summary.totalSeconds,
        sessions = summary.sessions,
        activeDays = days.size,
        bestStreak = streak(days, goals, LocalDate(year, 12, 31)).best,
        bestWeekday = weekday,
        bestDay = best.key,
        bestDaySeconds = best.value
    )
}
