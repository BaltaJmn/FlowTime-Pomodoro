package com.baltajmn.flowtime.core.design.sound

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundMixesTest {

    // El JSON de verdad, como las preferencias: así se prueba también que se lee lo que se guarda.
    private val json = mutableMapOf<SharedPreferencesItem, String>()
    private val prefs = mockk<DataProvider> {
        every { setString(any(), any()) } answers { json[firstArg()] = secondArg() }
        every { getString(any()) } answers { json[firstArg()] }
    }
    private var ids = 0
    private fun mixes() = SoundMixes(prefs) { "mix${++ids}" }

    private val rainy = mapOf(PlayerType.RAIN to 0.4f, PlayerType.FIRE to 0.8f)

    @Test
    fun `una mezcla guardada vuelve con los mismos volumenes, despues de las de ejemplo`() {
        mixes().save(" Lluvia y fuego ", rainy)

        val all = mixes().all.value
        assertEquals(SoundMixes.BUILT_IN, all.take(3))
        assertEquals(SoundMix("mix1", "Lluvia y fuego", rainy), all.last())
    }

    @Test
    fun `solo cuentan las del usuario, y borrar una de ejemplo solo la oculta`() {
        val mixes = mixes()
        mixes.save("Una", rainy)
        assertEquals(1, mixes.saved)

        mixes.delete(SoundMixes.BUILT_IN.first())
        assertEquals(1, mixes.saved)
        assertTrue(SoundMixes.BUILT_IN.first() !in mixes().all.value)

        mixes.delete(mixes.all.value.last())
        assertEquals(0, mixes().saved)
    }

    @Test
    fun `cambiar el nombre no toca los volumenes`() {
        val mixes = mixes()
        mixes.save("Una", rainy)
        mixes.rename("mix1", "Otra")
        assertEquals(SoundMix("mix1", "Otra", rainy), mixes().all.value.last())
    }

    @Test
    fun `de una copia se suman las que no estan, por nombre`() {
        val mixes = mixes()
        mixes.save("Una", rainy)
        mixes.restore(listOf("Una" to mapOf("WIND" to 1f), "Dos" to mapOf("WIND" to 1f, "YA_NO_EXISTE" to 1f)))

        assertEquals(listOf("Una", "Dos"), mixes.export().map { it.first })
        assertEquals(mapOf(PlayerType.WIND to 1f), mixes().all.value.last().volumes)
    }

    @Test
    fun `se leen las mezclas que se guardaban con Gson`() {
        json[SharedPreferencesItem.SOUND_MIXES] =
            """{"mixes":[{"id":"mix1","name":"Lluvia y fuego","volumes":{"RAIN":0.4,"FIRE":0.8}}],"hidden":["rainy_cafe"]}"""

        val all = mixes().all.value

        assertEquals(SoundMixes.BUILT_IN.drop(1) + SoundMix("mix1", "Lluvia y fuego", rainy), all)
    }
}
