package com.baltajmn.flowtime.core.persistence.sharedpreferences

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseJsonOrNullTest {

    private val rangeList = object : TypeToken<MutableList<RangeModel>>() {}.type

    @Test
    fun `un rango bien guardado se lee`() {
        val range = parseJsonOrNull<RangeModel>(
            """{"totalRange":0,"endRange":25,"rest":5}""",
            RangeModel::class.java
        )

        assertEquals(RangeModel(totalRange = 0, endRange = 25, rest = 5), range)
    }

    @Test
    fun `un rango con un numero roto no cierra la app`() {
        assertNull(parseJsonOrNull<RangeModel>("""{"endRange":"abc"}""", RangeModel::class.java))
    }

    @Test
    fun `una lista de rangos que no es una lista no cierra la app`() {
        assertNull(parseJsonOrNull<MutableList<RangeModel>>("""{"endRange":25}""", rangeList))
        assertNull(parseJsonOrNull<MutableList<RangeModel>>("no es json", rangeList))
    }

    @Test
    fun `sin valor guardado no hay rango`() {
        assertNull(parseJsonOrNull<RangeModel>(null, RangeModel::class.java))
    }
}
