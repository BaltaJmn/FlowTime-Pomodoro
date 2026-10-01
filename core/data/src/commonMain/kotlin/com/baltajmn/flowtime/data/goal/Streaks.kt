package com.baltajmn.flowtime.data.goal

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Desde [from], el objetivo es de [minutes] al día. */
data class GoalChange(val from: LocalDate, val minutes: Int)

data class Streak(
    /** Días seguidos cumpliendo el objetivo, hasta hoy (o hasta ayer, si hoy aún no se ha cumplido). */
    val current: Int = 0,
    val best: Int = 0,
    val todayMet: Boolean = false,
    /** El día libre de esta semana ya está gastado: otro día sin cumplir rompería la racha. */
    val freeDayUsedThisWeek: Boolean = false
)

object DailyGoal {
    const val DEFAULT_MINUTES = 60
    const val MIN_MINUTES = 10
    const val MAX_MINUTES = 480
    const val STEP_MINUTES = 5
}

/** El objetivo de [day]: el del último cambio hasta ese día, o el de por defecto si aún no había ninguno. */
fun goalOn(day: LocalDate, history: List<GoalChange>): Int =
    history.filter { it.from <= day }.maxByOrNull { it.from }?.minutes ?: DailyGoal.DEFAULT_MINUTES

/**
 * La racha, con el tiempo de cada día contado por el día en que empezó cada sesión: ni la medianoche
 * ni un cambio de zona horaria la mueven. Cada día se mide con el objetivo que tenía entonces.
 *
 * Un día sin cumplir se perdona si es el único de su semana (de lunes a domingo): no suma, pero no
 * rompe la racha. Solo se gasta si después hay otro día cumplido; si no, no hay racha que salvar. Hoy
 * suma en cuanto se cumple, y si aún no se ha cumplido no cuenta: el día no ha terminado.
 */
fun streak(secondsByDay: Map<LocalDate, Long>, history: List<GoalChange>, today: LocalDate): Streak {
    val first = secondsByDay.filterValues { it > 0 }.keys.minOrNull() ?: return Streak()
    fun met(day: LocalDate) = (secondsByDay[day] ?: 0) >= goalOn(day, history) * 60L

    val todayMet = met(today)
    val last = if (todayMet) today else today.minus(1, DateTimeUnit.DAY)
    var current = 0
    var best = 0
    // Las semanas cuyo día libre ya se ha gastado en esta racha, y las de los días sin cumplir que
    // aún no se sabe si se perdonan.
    val spent = mutableSetOf<LocalDate>()
    val pending = mutableSetOf<LocalDate>()
    var day = first
    while (day <= last) {
        if (met(day)) {
            spent += pending
            pending.clear()
            current++
            best = maxOf(best, current)
        } else if (current > 0) {
            val week = day.weekStart()
            if (week in spent || week in pending) {
                current = 0
                spent.clear()
                pending.clear()
            } else {
                pending += week
            }
        }
        day = day.plus(1, DateTimeUnit.DAY)
    }
    val thisWeek = today.weekStart()
    return Streak(
        current = current,
        best = best,
        todayMet = todayMet,
        freeDayUsedThisWeek = current > 0 && (thisWeek in spent || thisWeek in pending)
    )
}

/** El lunes de su semana: las semanas van de lunes a domingo en toda la app. */
fun LocalDate.weekStart() = minus(dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
