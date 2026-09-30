package com.baltajmn.flowtime.core.common.extensions

import kotlin.test.Test
import kotlin.test.assertEquals

class LongExtensionsTest {

    @Test
    fun menosDeUnaHoraSeVeComoMinutosYSegundos() {
        assertEquals("00:00", 0L.formatSecondsToTime())
        assertEquals("05:09", 309L.formatSecondsToTime())
        assertEquals("59:59", 3599L.formatSecondsToTime())
    }

    @Test
    fun conHorasSeAnadenDelante() {
        assertEquals("01:00:00", 3600L.formatSecondsToTime())
        assertEquals("10:02:03", 36123L.formatSecondsToTime())
    }

    @Test
    fun losMinutosEstudiadosSeLeenEnHorasYMinutos() {
        assertEquals("45 min", 45L.formatMinutesStudying())
        assertEquals("2 h", 120L.formatMinutesStudying())
        assertEquals("1 h 5 min", 65L.formatMinutesStudying())
    }
}
