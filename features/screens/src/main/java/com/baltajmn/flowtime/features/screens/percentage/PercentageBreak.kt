package com.baltajmn.flowtime.features.screens.percentage

/** Descanso del modo Porcentaje: ese porcentaje de lo trabajado, redondeado al segundo por abajo. */
fun percentageBreakSeconds(workedSeconds: Long, percentage: Long): Long =
    workedSeconds * percentage / 100
