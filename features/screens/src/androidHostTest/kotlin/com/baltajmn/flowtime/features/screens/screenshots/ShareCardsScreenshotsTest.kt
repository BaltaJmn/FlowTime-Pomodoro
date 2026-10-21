package com.baltajmn.flowtime.features.screens.screenshots

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.data.stats.YearRecap
import com.baltajmn.flowtime.features.screens.recap.YearRecordsCard
import com.baltajmn.flowtime.features.screens.recap.YearTotalsCard
import com.baltajmn.flowtime.features.screens.share.DayShareCard
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las tarjetas para compartir (#83 y #85) tal como salen en la hoja: 360 dp de ancho, en claro y en oscuro. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ShareCardsScreenshotsTest(private val dark: Boolean) {

    @get:Rule(order = 0)
    val onlyWithRoborazzi = RoborazziOnly()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val mode get() = if (dark) "dark" else "light"

    private val recap = YearRecap(
        year = 2026,
        totalSeconds = 142 * 3600L + 20 * 60,
        sessions = 311,
        activeDays = 187,
        bestStreak = 23,
        bestWeekday = DayOfWeek.WEDNESDAY,
        bestDay = LocalDate(2026, 5, 14),
        bestDaySeconds = 7 * 3600L + 40 * 60
    )

    @Test
    fun day() = compose.capture("share_day_$mode", dark = dark) {
        Box(modifier = Modifier.width(360.dp)) {
            DayShareCard(day = LocalDate(2026, 10, 21), seconds = 85 * 60L, sessions = 4, streak = 6)
        }
    }

    @Test
    fun yearTotals() = compose.capture("share_year_totals_$mode", dark = dark) {
        Box(modifier = Modifier.width(360.dp)) { YearTotalsCard(recap) }
    }

    @Test
    fun yearRecords() = compose.capture("share_year_records_$mode", dark = dark) {
        Box(modifier = Modifier.width(360.dp)) { YearRecordsCard(recap) }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "oscuro={0}")
        fun parameters() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }
}
