package com.baltajmn.flowtime.data.timer

import com.baltajmn.flowtime.core.persistence.model.RangeModel

private const val DEFAULT_BREAK_SECONDS = 15 * 60L

/**
 * Descanso de FlowTime según lo trabajado. Cada rango cubre hasta su totalRange, límite incluido,
 * y el último ("y después de N minutos") cubre todo lo que pase del anterior.
 */
fun flowTimeBreakSeconds(workedSeconds: Long, ranges: List<RangeModel>): Long {
    ranges.forEachIndexed { index, range ->
        val isLast = index == ranges.lastIndex
        if (isLast || workedSeconds <= range.totalRange * 60L) return range.rest * 60L
    }
    return DEFAULT_BREAK_SECONDS
}

/**
 * Recalcula el totalRange de cada rango como la suma de las duraciones hasta él, que es lo que
 * muestran los ajustes. Borrar un rango intermedio dejaba los siguientes con el total antiguo.
 */
fun List<RangeModel>.withCumulativeTotals(): MutableList<RangeModel> {
    var total = 0
    return map { range ->
        val duration = range.endRange.coerceAtLeast(1)
        total += duration
        range.copy(totalRange = total, endRange = duration)
    }.toMutableList()
}

/**
 * El tramo de FlowTime en el que se está: empezó a los [fromSeconds] de trabajo y termina a los
 * [atSeconds]; a partir de ahí, el descanso es de [breakSeconds].
 */
data class NextStep(val fromSeconds: Long, val atSeconds: Long, val breakSeconds: Long)

/**
 * El siguiente cambio de descanso de FlowTime, con las mismas reglas que [flowTimeBreakSeconds]: el
 * límite de cada tramo es suyo, y el descanso cambia al pasarlo. null tras el último límite, cuando
 * el descanso ya no cambia más. [ranges] con los totales acumulados, como los usa el motor.
 */
fun flowTimeNextStep(workedSeconds: Long, ranges: List<RangeModel>): NextStep? {
    ranges.dropLast(1).forEachIndexed { index, range ->
        val at = range.totalRange * 60L
        if (workedSeconds <= at) {
            val from = if (index == 0) 0L else ranges[index - 1].totalRange * 60L
            return NextStep(from, at, ranges[index + 1].rest * 60L)
        }
    }
    return null
}

/** Descanso del modo Porcentaje: ese porcentaje de lo trabajado, redondeado al segundo por abajo. */
fun percentageBreakSeconds(workedSeconds: Long, percentage: Long): Long =
    workedSeconds * percentage / 100
