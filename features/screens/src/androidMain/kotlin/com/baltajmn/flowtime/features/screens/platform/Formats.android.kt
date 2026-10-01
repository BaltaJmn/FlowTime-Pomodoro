package com.baltajmn.flowtime.features.screens.platform

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.toJavaDayOfWeek
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toJavaLocalTime
import kotlinx.datetime.toJavaMonth
import kotlinx.datetime.toKotlinDayOfWeek

actual object Formats {
    private val locale: Locale get() = Locale.getDefault()

    actual fun mediumDate(date: LocalDate): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date.toJavaLocalDate())

    actual fun monthYear(date: LocalDate): String = DateTimeFormatter.ofPattern("LLLL yyyy").format(date.toJavaLocalDate())

    actual fun weekdayDay(date: LocalDate): String = DateTimeFormatter.ofPattern("EEEE d").format(date.toJavaLocalDate())

    actual fun shortMonth(month: Month): String = month.toJavaMonth().getDisplayName(TextStyle.SHORT, locale)

    actual fun narrowMonth(month: Month): String = month.toJavaMonth().getDisplayName(TextStyle.NARROW, locale)

    actual fun shortWeekday(day: DayOfWeek): String = day.toJavaDayOfWeek().getDisplayName(TextStyle.SHORT, locale)

    actual fun narrowWeekday(day: DayOfWeek): String = day.toJavaDayOfWeek().getDisplayName(TextStyle.NARROW, locale)

    actual fun shortTime(time: LocalTime): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(time.toJavaLocalTime())

    actual fun percent(value: Float): String = NumberFormat.getPercentInstance().format(value)

    actual fun firstDayOfWeek(): DayOfWeek = WeekFields.of(locale).firstDayOfWeek.toKotlinDayOfWeek()
}
