package com.baltajmn.flowtime.features.screens.todoList

import com.baltajmn.flowtime.features.screens.platform.Formats
import kotlinx.datetime.LocalDate

// Los nombres del mes y del día, en el idioma del móvil.
fun LocalDate.toShowInList() = "$day ${Formats.shortMonth(month)}, ${Formats.shortWeekday(dayOfWeek)}"