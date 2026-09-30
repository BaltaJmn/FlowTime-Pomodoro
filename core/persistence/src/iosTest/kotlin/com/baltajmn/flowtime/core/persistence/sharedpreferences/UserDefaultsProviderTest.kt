package com.baltajmn.flowtime.core.persistence.sharedpreferences

import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DARK_MODE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DAILY_GOAL
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.THEME_COLOR
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import platform.Foundation.NSUserDefaults

class UserDefaultsProviderTest {

    private val suite = "UserDefaultsProviderTest"
    private val prefs = UserDefaultsProvider(NSUserDefaults(suiteName = suite))

    @AfterTest
    fun clean() {
        NSUserDefaults.standardUserDefaults.removePersistentDomainForName(suite)
    }

    @Test
    fun withNothingStoredTheDefaultsApply() {
        assertNull(prefs.getString(THEME_COLOR))
        assertTrue(prefs.getBoolean(DARK_MODE, defValue = true))
        assertFalse(prefs.getBoolean(DARK_MODE, defValue = false))
        assertEquals(0L, prefs.getLong(DAILY_GOAL))
        assertEquals(0.5f, prefs.getFloat("rain", 0.5f))
        assertNull(prefs.getObject<RangeModel>(POMODORO_RANGE))
    }

    @Test
    fun whatIsStoredIsRead() {
        prefs.setString(THEME_COLOR, "Blue")
        prefs.setBoolean(DARK_MODE, false)
        prefs.setLong(DAILY_GOAL, 7_200_000_000L)
        prefs.setFloat("rain", 0f)
        prefs.setObject(POMODORO_RANGE, RangeModel(totalRange = 25, endRange = 25, rest = 5))

        assertEquals("Blue", prefs.getString(THEME_COLOR))
        assertFalse(prefs.getBoolean(DARK_MODE, defValue = true))
        assertEquals(7_200_000_000L, prefs.getLong(DAILY_GOAL))
        assertEquals(0f, prefs.getFloat("rain", 0.5f))
        assertEquals(RangeModel(25, 25, 5), prefs.getObject<RangeModel>(POMODORO_RANGE))
    }
}
