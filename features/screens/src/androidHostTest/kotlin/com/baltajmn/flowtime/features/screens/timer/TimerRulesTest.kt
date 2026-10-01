package com.baltajmn.flowtime.features.screens.timer

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.data.timer.NextStep
import com.baltajmn.flowtime.data.timer.flowTimeBreakSeconds
import com.baltajmn.flowtime.data.timer.flowTimeNextStep
import com.baltajmn.flowtime.data.timer.percentageBreakSeconds
import com.baltajmn.flowtime.data.timer.withCumulativeTotals
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerRulesTest {

    @Test
    fun `el descanso del modo Porcentaje es el porcentaje de lo trabajado`() {
        assertEquals(10L, percentageBreakSeconds(workedSeconds = 50, percentage = 20))
        assertEquals(1188L, percentageBreakSeconds(workedSeconds = 3600, percentage = 33))
    }

    @Test
    fun `el descanso del modo Porcentaje se redondea por abajo al segundo`() {
        assertEquals(11L, percentageBreakSeconds(workedSeconds = 59, percentage = 20))
    }

    // 0-15 min: 5 de descanso; 15-30: 10; y después de 30: 15.
    private val defaults = TimerDefaults.flowTimeRanges()

    private fun minutes(value: Int) = value * 60L

    @Test
    fun `cada duracion cae en su rango`() {
        assertEquals(minutes(5), flowTimeBreakSeconds(minutes(10), defaults))
        assertEquals(minutes(10), flowTimeBreakSeconds(minutes(20), defaults))
        assertEquals(minutes(15), flowTimeBreakSeconds(minutes(45), defaults))
    }

    @Test
    fun `los limites exactos pertenecen al rango que terminan`() {
        assertEquals(minutes(5), flowTimeBreakSeconds(minutes(15), defaults))
        assertEquals(minutes(10), flowTimeBreakSeconds(minutes(15) + 1, defaults))
        assertEquals(minutes(10), flowTimeBreakSeconds(minutes(30), defaults))
        assertEquals(minutes(15), flowTimeBreakSeconds(minutes(30) + 1, defaults))
    }

    @Test
    fun `se respeta el descanso configurado en el ultimo rango`() {
        val ranges = defaults.toMutableList().apply { this[lastIndex] = last().copy(rest = 25) }

        assertEquals(minutes(25), flowTimeBreakSeconds(minutes(45), ranges))
        assertEquals(minutes(25), flowTimeBreakSeconds(minutes(180), ranges))
    }

    @Test
    fun `el ultimo rango se aplica aunque su total guardado sea menor que el anterior`() {
        // Los rangos por defecto de versiones anteriores guardaban 15 en el último.
        val legacy = listOf(
            RangeModel(totalRange = 15, endRange = 15, rest = 5),
            RangeModel(totalRange = 30, endRange = 15, rest = 10),
            RangeModel(totalRange = 15, endRange = 15, rest = 20)
        )

        assertEquals(minutes(20), flowTimeBreakSeconds(minutes(40), legacy))
    }

    @Test
    fun `sin rangos se descansa 15 minutos`() {
        assertEquals(minutes(15), flowTimeBreakSeconds(minutes(40), emptyList()))
    }

    @Test
    fun `los totales se recalculan a partir de las duraciones`() {
        val stale = listOf(
            RangeModel(totalRange = 15, endRange = 15, rest = 5),
            RangeModel(totalRange = 45, endRange = 15, rest = 10),
            RangeModel(totalRange = 15, endRange = 15, rest = 15)
        )

        val totals = stale.withCumulativeTotals().map { it.totalRange }

        assertEquals(listOf(15, 30, 45), totals)
    }

    @Test
    fun `una duracion de 0 cuenta como 1 minuto para que los limites crezcan`() {
        val ranges = listOf(
            RangeModel(totalRange = 10, endRange = 10, rest = 5),
            RangeModel(totalRange = 10, endRange = 0, rest = 10)
        )

        assertEquals(listOf(10, 11), ranges.withCumulativeTotals().map { it.totalRange })
    }

    @Test
    fun `el siguiente tramo de FlowTime, en el borde de cada uno`() {
        val ranges = defaults.withCumulativeTotals()

        assertEquals(NextStep(0, minutes(15), minutes(10)), flowTimeNextStep(minutes(10), ranges))
        // El límite es del tramo que termina, como en el descanso.
        assertEquals(NextStep(0, minutes(15), minutes(10)), flowTimeNextStep(minutes(15), ranges))
        assertEquals(
            NextStep(minutes(15), minutes(30), minutes(15)),
            flowTimeNextStep(minutes(15) + 1, ranges)
        )
        // Y lo que dice coincide con el descanso que da el motor al pasarlo.
        assertEquals(minutes(15), flowTimeBreakSeconds(minutes(30) + 1, ranges))
    }

    @Test
    fun `tras el ultimo limite el descanso ya no cambia`() {
        assertEquals(null, flowTimeNextStep(minutes(30) + 1, defaults.withCumulativeTotals()))
        assertEquals(null, flowTimeNextStep(minutes(3), listOf(RangeModel(20, 20, 5))))
    }

    @Test
    fun `con un tramo borrado a mitad cuentan los totales de nuevo`() {
        // Borrado el de 15 a 30, el de 45 se queda con 15 minutos: termina a los 30.
        val ranges = listOf(RangeModel(15, 15, 5), RangeModel(45, 15, 15), RangeModel(60, 15, 20))
            .withCumulativeTotals()

        assertEquals(
            NextStep(minutes(15), minutes(30), minutes(20)),
            flowTimeNextStep(minutes(20), ranges)
        )
    }
}
