package com.baltajmn.flowtime.core.persistence.sharedpreferences

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.google.gson.Gson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.serializer

/** Hasta la 2.1.0 los rangos se guardaban con Gson: tienen que seguir leyéndose. */
class GsonCompatibilityTest {

    @Test
    fun `se lee un rango guardado con Gson`() {
        val range = RangeModel(totalRange = 50, endRange = 50, rest = 10)

        assertEquals(range, parseJsonOrNull(Gson().toJson(range), serializer<RangeModel>()))
    }

    @Test
    fun `se lee una lista de rangos guardada con Gson`() {
        val ranges = mutableListOf(RangeModel(15, 15, 5), RangeModel(30, 15, 10))

        assertEquals(ranges, parseJsonOrNull(Gson().toJson(ranges), serializer<MutableList<RangeModel>>()))
    }
}
