package com.baltajmn.flowtime.features.screens.todoList

import java.time.format.TextStyle
import java.util.Locale
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate

fun LocalDate.toShowInList() = buildTodayLabel(this)

private fun buildTodayLabel(shown: LocalDate): String {
    // Los nombres del mes y del día, en el idioma del móvil: de java.time hasta que la pantalla sea común.
    val date = shown.toJavaLocalDate()
    val day = date.dayOfMonth
    val month = date.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    return "$day $month, $dayOfWeek"
}