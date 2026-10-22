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
        assertEquals("45 min", 45L.formatMinutesStudying("en"))
        assertEquals("2 h", 120L.formatMinutesStudying("en"))
        assertEquals("1 h 5 min", 65L.formatMinutesStudying("en"))
    }

    @Test
    fun lasUnidadesSonLasDelIdioma() {
        assertEquals("0 мин", 0L.formatMinutesStudying("ru"))
        assertEquals("1 ч 5 мин", 65L.formatMinutesStudying("ru"))
        assertEquals("2 Std.", 120L.formatMinutesStudying("de"))
        assertEquals("1時間5分", 65L.formatMinutesStudying("ja"))
        assertEquals("1시간 5분", 65L.formatMinutesStudying("ko"))
        assertEquals(65L.formatMinutesStudying("id"), 65L.formatMinutesStudying("in"))
        // Un idioma sin traducir, como el resto de textos: en inglés.
        assertEquals("1 h 5 min", 65L.formatMinutesStudying("sv"))
    }
}
