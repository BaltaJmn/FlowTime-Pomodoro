package com.baltajmn.flowtime.core.common.extensions

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun LocalDate.toShowInSelector() = buildWeekLabel(this)
fun LocalDate.toShowInList() = buildTodayLabel(this)

private fun buildWeekLabel(date: LocalDate): String {
    val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val endOfWeek = startOfWeek.plusDays(6)
    val startMonth = startOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val endMonth = endOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    return "${startOfWeek.dayOfMonth} $startMonth - ${endOfWeek.dayOfMonth} $endMonth"
}

private fun buildTodayLabel(date: LocalDate): String {
    val day = date.dayOfMonth
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    return "$day $month, $dayOfWeek"
}