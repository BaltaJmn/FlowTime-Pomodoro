package com.baltajmn.flowtime.features.screens.todoList

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

fun LocalDate.toShowInList() = buildTodayLabel(this)

private fun buildTodayLabel(date: LocalDate): String {
    val day = date.dayOfMonth
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    return "$day $month, $dayOfWeek"
}