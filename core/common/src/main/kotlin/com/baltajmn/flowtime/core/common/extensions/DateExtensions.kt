package com.baltajmn.flowtime.core.common.extensions

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

fun LocalDate.toShowInSelector() = buildWeekLabel(this)

private fun buildWeekLabel(date: LocalDate): String {
    val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val endOfWeek = startOfWeek.plusDays(6)
    val startMonth = startOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val endMonth = endOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    return "${startOfWeek.dayOfMonth} $startMonth - ${endOfWeek.dayOfMonth} $endMonth"
}