package com.baltajmn.flowtime.features.screens.flowtime

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
