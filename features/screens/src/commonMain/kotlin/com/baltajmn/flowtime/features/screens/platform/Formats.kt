package com.baltajmn.flowtime.features.screens.platform

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month

/**
 * Fechas, horas y porcentajes con el formato y el idioma del móvil. Se piden al pintar: si cambia el
 * idioma, el siguiente texto ya sale en el nuevo.
 */
expect object Formats {
    /** "1 oct 2026". */
    fun mediumDate(date: LocalDate): String

    /** "octubre de 2026". */
    fun monthYear(date: LocalDate): String

    /** "jueves 1". */
    fun weekdayDay(date: LocalDate): String

    fun shortMonth(month: Month): String

    fun narrowMonth(month: Month): String

    fun shortWeekday(day: DayOfWeek): String

    fun narrowWeekday(day: DayOfWeek): String

    /** "18:00" o "6:00 PM", según el idioma. */
    fun shortTime(time: LocalTime): String

    /** "12 %", con el formato de porcentaje del idioma. */
    fun percent(value: Float): String

    /** El primer día de la semana en el calendario del idioma. */
    fun firstDayOfWeek(): DayOfWeek
}
