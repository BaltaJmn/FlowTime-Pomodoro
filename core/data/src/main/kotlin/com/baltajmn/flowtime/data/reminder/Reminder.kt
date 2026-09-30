package com.baltajmn.flowtime.data.reminder

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DAILY_REMINDER
import com.baltajmn.flowtime.core.persistence.sharedpreferences.getObject
import com.baltajmn.flowtime.core.persistence.sharedpreferences.setObject
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/** El recordatorio diario (#45): apagado de entrada; al encenderlo, a las 18:00 de lunes a viernes. */
@Serializable
data class Reminder(
    val enabled: Boolean = false,
    val minuteOfDay: Int = 18 * 60,
    /** Un bit por día, con el lunes en el primero. */
    val days: Int = WEEKDAYS
) {
    val time: LocalTime get() = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)

    fun on(day: DayOfWeek) = days and day.bit != 0

    fun toggle(day: DayOfWeek) = copy(days = days xor day.bit)

    /**
     * La próxima vez que toca, justo después de [now] y en su zona horaria; null si está apagado o
     * sin días. Con el cambio de hora, una hora que no existe ese día pasa a la siguiente que sí.
     */
    fun next(now: ZonedDateTime): ZonedDateTime? {
        if (!enabled || days == 0) return null
        return (0L..7L).asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { on(it.dayOfWeek) }
            .map { ZonedDateTime.of(it, time, now.zone) }
            .first { it.isAfter(now) }
    }

    companion object {
        const val WEEKDAYS = 0b0011111
        private val DayOfWeek.bit get() = 1 shl (value - 1)
    }
}

/** Lo que dice el recordatorio, según cómo vaya el día. */
sealed interface Nudge {
    /** Ya hay algo hecho: lo que falta para el objetivo. */
    data class MinutesLeft(val minutes: Long) : Nudge

    /** Nada hecho hoy, pero hay una racha que no perder. */
    data class KeepStreak(val days: Int) : Nudge

    data object ShortSession : Nudge
}

/** Nada si el objetivo ya está cumplido o hay una sesión en marcha. */
fun nudge(today: DayProgress, streak: Streak, sessionActive: Boolean): Nudge? = when {
    sessionActive || today.met -> null
    today.seconds > 0 -> Nudge.MinutesLeft((today.goalMinutes * 60L - today.seconds + 59) / 60)
    streak.current >= 2 -> Nudge.KeepStreak(streak.current)
    else -> Nudge.ShortSession
}

class ReminderRepository(private val prefs: DataProvider) {
    private val _reminder = MutableStateFlow(prefs.getObject<Reminder>(DAILY_REMINDER) ?: Reminder())
    val reminder: StateFlow<Reminder> = _reminder.asStateFlow()

    fun set(reminder: Reminder) {
        prefs.setObject(DAILY_REMINDER, reminder)
        _reminder.value = reminder
    }
}
