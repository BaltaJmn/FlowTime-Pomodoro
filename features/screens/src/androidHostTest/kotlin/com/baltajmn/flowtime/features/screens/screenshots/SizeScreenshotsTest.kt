package com.baltajmn.flowtime.features.screens.screenshots

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.focus.FocusContent
import com.baltajmn.flowtime.features.screens.onboard.OnBoardUiState
import com.baltajmn.flowtime.features.screens.onboard.OnboardingContent
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import com.baltajmn.flowtime.features.screens.settings.SettingsContent
import com.baltajmn.flowtime.features.screens.stats.StatsContent
import com.baltajmn.flowtime.features.screens.todoList.TodoListContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Las pantallas principales en cada tamaño de ventana (#46), en claro: que nada se estire ni se
 * corte. Cada subclase pone el tamaño.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class SizeScreenshotsTest(private val size: String) {

    @get:Rule(order = 0)
    val onlyWithRoborazzi = RoborazziOnly()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    @Test
    fun focus() = compose.capture("${size}_focus") {
        FocusContent(
            state = sampleFocus,
            showSound = true,
            soundPlaying = false,
            onSelectMode = {},
            onAction = {},
            onOpenSounds = {},
            onOpenModeSettings = {}
        )
    }

    @Test
    fun stats() = compose.capture("${size}_stats") {
        StatsContent(
            state = sampleStats(ProAccess.OPEN),
            onPeriod = {},
            onPrevious = {},
            onNext = {},
            onUnlock = {},
            onDismissProCard = {},
            onCopyHistory = {},
            onPasteHistory = {},
            onExportCsv = {}
        )
    }

    @Test
    fun todoList() {
        val viewModel = sampleTodo()
        compose.capture("${size}_todo") {
            val state by viewModel.uiState.collectAsState()
            TodoListContent(
                state = state,
                listState = rememberLazyListState(),
                viewModel = viewModel,
                onSeePro = {}
            )
        }
    }

    @Test
    fun settings() {
        val viewModel = sampleSettings()
        compose.capture("${size}_settings") {
            val state by viewModel.uiState.collectAsState()
            SettingsContent(
                state = state,
                listState = rememberLazyListState(),
                viewModel = viewModel,
                showSound = true,
                onSoundChange = {},
                navigateToIntro = {},
                onOpenPro = {}
            )
        }
    }

    @Test
    fun onboarding() = compose.capture("${size}_onboarding") {
        OnboardingContent(
            state = OnBoardUiState(mode = TimerMode.FLOW_TIME, goalMinutes = 60),
            onMode = {},
            onGoal = {},
            onSkip = {},
            onStart = {}
        )
    }
}

@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class PhoneScreenshotsTest : SizeScreenshotsTest("phone")

@Config(sdk = [34], qualifiers = "w891dp-h411dp")
class PhoneLandscapeScreenshotsTest : SizeScreenshotsTest("phone_land")

@Config(sdk = [34], qualifiers = "w673dp-h841dp")
class FoldScreenshotsTest : SizeScreenshotsTest("fold")

@Config(sdk = [34], qualifiers = "w1280dp-h800dp")
class TabletScreenshotsTest : SizeScreenshotsTest("tablet")
