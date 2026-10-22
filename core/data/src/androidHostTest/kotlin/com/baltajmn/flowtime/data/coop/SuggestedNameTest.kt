package com.baltajmn.flowtime.data.coop

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestedNameTest {

    @Test
    fun `el primer nombre de la cuenta`() {
        assertEquals("Ana", suggestedName(buildJsonObject { put("full_name", "Ana Perez Lopez") }, random = 1234))
        assertEquals("Leo", suggestedName(buildJsonObject { put("name", " Leo ") }, random = 1234))
    }

    @Test
    fun `sin nombre en la cuenta, como Apple, uno por defecto valido`() {
        assertEquals("Focus1234", suggestedName(null, random = 1234))
        assertEquals("Focus1234", suggestedName(buildJsonObject { put("email", "a@b.c") }, random = 1234))
        assertEquals("Focus1234", suggestedName(buildJsonObject { put("full_name", "  ") }, random = 1234))
    }

    @Test
    fun `nunca pasa de 30 y siempre es valido`() {
        val long = "A".repeat(50)
        assertEquals(30, suggestedName(buildJsonObject { put("full_name", long) }).length)
        assertEquals(true, isValidName(suggestedName(null)))
    }
}
