package com.baltajmn.flowtime.features.screens.screenshots

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerHint
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.focus.FocusContent
import com.baltajmn.flowtime.features.screens.focus.FocusUiState
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** La pantalla de concentración con cada tema, en claro y en oscuro, para revisarlos de un vistazo. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ThemeScreenshotsTest(private val theme: AppTheme, private val dark: Boolean) {

    @get:Rule(order = 0)
    val onlyWithRoborazzi = RoborazziOnly()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun timer() = compose.capture(
        name = "theme_${theme.name.lowercase()}_${if (dark) "dark" else "light"}",
        theme = theme,
        dark = dark
    ) {
        FocusContent(
            state = FocusUiState(
                mode = TimerMode.FLOW_TIME,
                phase = Phase.WORK,
                time = "24:59",
                minutesToday = "1 h 25 min",
                goalToday = "2 h",
                progress = 0.66f,
                hint = TimerHint.NextStep(atMinutes = 30, breakMinutes = 15),
                streak = 4
            ),
            showSound = true,
            soundPlaying = true,
            onSelectMode = {},
            onAction = {},
            onOpenSounds = {},
            onOpenModeSettings = {}
        )
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0} oscuro={1}")
        fun parameters() = AppTheme.entries.flatMap { theme ->
            listOf(arrayOf<Any>(theme, false), arrayOf<Any>(theme, true))
        }
    }
}
