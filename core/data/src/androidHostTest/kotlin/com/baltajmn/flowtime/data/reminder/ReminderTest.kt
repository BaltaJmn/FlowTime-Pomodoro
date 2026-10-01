package com.baltajmn.flowtime.data.reminder

import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.offsetAt
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant

class ReminderTest {

    private val madrid = TimeZone.of("Europe/Madrid")
    private fun at(text: String) = LocalDateTime.parse(text).toInstant(madrid)

    private fun Reminder.next(now: Instant) = next(now, madrid)

    // El 30 de septiembre de 2026 es miércoles.
    private val weekdays = Reminder(enabled = true)

    @Test
    fun `apagado o sin dias no toca nunca`() {
        assertNull(Reminder().next(at("2026-09-30T10:00")))
        assertNull(weekdays.copy(days = 0).next(at("2026-09-30T10:00")))
    }

    @Test
    fun `antes de la hora toca hoy, y justo a la hora o despues, el siguiente dia marcado`() {
        assertEquals(at("2026-09-30T18:00"), weekdays.next(at("2026-09-30T10:00")))
        assertEquals(at("2026-10-01T18:00"), weekdays.next(at("2026-09-30T18:00")))
        // El viernes por la tarde salta el fin de semana.
        assertEquals(at("2026-10-05T18:00"), weekdays.next(at("2026-10-02T19:00")))
    }

    @Test
    fun `con dias salteados va al siguiente marcado`() {
        val mondays = Reminder(enabled = true, minuteOfDay = 7 * 60 + 30, days = 0)
            .toggle(DayOfWeek.MONDAY)
        assertEquals(at("2026-10-05T07:30"), mondays.next(at("2026-09-30T10:00")))
        // Un lunes después de la hora: el lunes siguiente.
        assertEquals(at("2026-10-12T07:30"), mondays.next(at("2026-10-05T08:00")))
    }

    @Test
    fun `con el cambio de hora sigue a la misma hora de reloj`() {
        // El 25 de octubre de 2026 se retrasa la hora en Madrid: de +02:00 a +01:00.
        val daily = Reminder(enabled = true, days = 0b1111111)
        val next = daily.next(at("2026-10-24T19:00"))!!
        assertEquals(LocalDateTime.parse("2026-10-25T18:00"), next.toLocalDateTime(madrid))
        assertEquals(3600, madrid.offsetAt(next).totalSeconds)
        // El 29 de marzo de 2026 las 02:30 no existen: pasa a las 03:30.
        val night = Reminder(enabled = true, minuteOfDay = 2 * 60 + 30, days = 0b1111111)
        assertEquals(LocalDateTime.parse("2026-03-29T03:30"), night.next(at("2026-03-29T01:00"))!!.toLocalDateTime(madrid))
    }

    @Test
    fun `el texto depende de como vaya el dia, y no avisa con el objetivo cumplido ni con una sesion`() {
        val day = LocalDate(2026, 9, 30)
        val nothing = DayProgress(day, seconds = 0, goalMinutes = 60)
        val some = DayProgress(day, seconds = 40 * 60 + 1, goalMinutes = 60)
        val done = DayProgress(day, seconds = 60 * 60, goalMinutes = 60)

        assertEquals(Nudge.MinutesLeft(20), nudge(some, Streak(current = 5), sessionActive = false))
        assertEquals(Nudge.KeepStreak(6), nudge(nothing, Streak(current = 6), sessionActive = false))
        assertEquals(Nudge.ShortSession, nudge(nothing, Streak(current = 1), sessionActive = false))
        assertNull(nudge(done, Streak(), sessionActive = false))
        assertNull(nudge(nothing, Streak(), sessionActive = true))
    }

    @Test
    fun `se guarda y se vuelve a leer`() {
        val prefs = FakeDataProvider()
        val changed = Reminder(enabled = true, minuteOfDay = 21 * 60).toggle(DayOfWeek.SUNDAY)
        ReminderRepository(prefs).set(changed)

        assertEquals(changed, ReminderRepository(prefs).reminder.value)
    }
}
