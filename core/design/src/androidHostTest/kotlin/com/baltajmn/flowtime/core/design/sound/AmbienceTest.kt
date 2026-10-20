package com.baltajmn.flowtime.core.design.sound

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class AmbienceTest {

    private val prefs = mockk<DataProvider>(relaxed = true) {
        every { getFloat(any(), any()) } answers { secondArg() }
    }
    private val ambience = Ambience(prefs, mockk(relaxed = true)) {}

    @Test
    fun `reproducir y pausa de En reproduccion alternan y siguen ensenando los mismos sonidos`() {
        ambience.play(PlayerType.RAIN, true)
        ambience.play(PlayerType.BROWN, true)
        val both = setOf(PlayerType.RAIN, PlayerType.BROWN)

        ambience.toggle()
        assertEquals(emptySet<PlayerType>(), ambience.state.value.playing)
        assertEquals(both, ambience.state.value.shown)

        ambience.toggle()
        assertEquals(both, ambience.state.value.playing)
        assertEquals(both, ambience.state.value.shown)
    }

    @Test
    fun `parado no ensena nada y el boton no arranca nada`() {
        ambience.play(PlayerType.RAIN, true)
        ambience.stop()

        ambience.toggle()
        assertEquals(emptySet<PlayerType>(), ambience.state.value.shown)
    }
}
