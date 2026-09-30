package com.baltajmn.flowtime.core.persistence.sharedpreferences

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.serializer

class StoredObjectsTest {

    private val range = serializer<RangeModel>()
    private val rangeList = serializer<MutableList<RangeModel>>()

    @Test
    fun aStoredRangeIsRead() {
        val stored = parseJsonOrNull("""{"totalRange":0,"endRange":25,"rest":5}""", range)

        assertEquals(RangeModel(totalRange = 0, endRange = 25, rest = 5), stored)
    }

    @Test
    fun aRangeWithABrokenNumberDoesNotCrash() {
        assertNull(parseJsonOrNull("""{"endRange":"abc"}""", range))
    }

    @Test
    fun aRangeListThatIsNotAListDoesNotCrash() {
        assertNull(parseJsonOrNull("""{"endRange":25}""", rangeList))
        assertNull(parseJsonOrNull("no es json", rangeList))
    }

    @Test
    fun nothingStoredIsNoRange() {
        assertNull(parseJsonOrNull(null, range))
    }

    @Test
    fun fieldsFromOtherVersionsAreIgnored() {
        val stored = parseJsonOrNull("""{"totalRange":15,"endRange":15,"rest":5,"new":true}""", range)

        assertEquals(RangeModel(totalRange = 15, endRange = 15, rest = 5), stored)
    }
}
