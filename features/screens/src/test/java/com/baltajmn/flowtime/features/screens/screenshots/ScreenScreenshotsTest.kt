package com.baltajmn.flowtime.features.screens.screenshots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.components.ExpandedContent
import com.baltajmn.flowtime.core.design.sound.PlayerState
import com.baltajmn.flowtime.core.design.sound.PlayerType
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.focus.FocusContent
import com.baltajmn.flowtime.features.screens.focus.FocusUiState
import com.baltajmn.flowtime.features.screens.fakes.FakePurchases
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import com.baltajmn.flowtime.features.screens.pro.ProContent
import com.baltajmn.flowtime.features.screens.pro.ProFeature
import com.baltajmn.flowtime.features.screens.pro.ProUiState
import com.baltajmn.flowtime.features.screens.stats.StatsContent
import com.baltajmn.flowtime.features.screens.stats.StatsUiState
import com.baltajmn.flowtime.features.screens.settings.AppIcon
import com.baltajmn.flowtime.features.screens.settings.AppearanceCard
import com.baltajmn.flowtime.features.screens.settings.SettingsContent
import com.baltajmn.flowtime.features.screens.support.SupportContent
import com.baltajmn.flowtime.features.screens.support.SupportState
import com.baltajmn.flowtime.features.screens.todoList.TodoListContent
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Las pantallas principales con el tema azul, en claro y en oscuro. */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreenScreenshotsTest(private val dark: Boolean) {

    @get:Rule(order = 0)
    val onlyWithRoborazzi = RoborazziOnly()

    @get:Rule(order = 1)
    val compose = createComposeRule()

    private val mode get() = if (dark) "dark" else "light"

    @Test
    fun focus() = compose.capture("focus_$mode", dark = dark) {
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
    @Config(qualifiers = "+land")
    fun focusLandscape() = compose.capture("focus_landscape_$mode", dark = dark) {
        FocusContent(
            state = FocusUiState(
                mode = TimerMode.POMODORO,
                phase = Phase.BREAK,
                time = "04:12",
                minutesToday = "45 min",
                goalToday = "1 h",
                progress = 0.28f
            ),
            showSound = true,
            soundPlaying = true,
            onSelectMode = {},
            onAction = {},
            onOpenSounds = {},
            onOpenModeSettings = {}
        )
    }

    @Test
    fun settings() {
        val viewModel = sampleSettings()
        compose.capture("settings_$mode", dark = dark) {
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
    @Config(qualifiers = "+h1700dp")
    fun stats() = compose.capture("stats_$mode", dark = dark) {
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
    @Config(qualifiers = "+h1700dp")
    fun statsLocked() = compose.capture("stats_locked_$mode", dark = dark) {
        StatsContent(
            // Con la racha de 7 días, sale también la tarjeta de Pro.
            state = sampleStats(ProAccess.LOCKED).copy(
                streak = Streak(current = 7, best = 9),
                proCardAllowed = true
            ),
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
    @Config(qualifiers = "+h1000dp")
    fun pro() = compose.capture("pro_$mode", dark = dark) {
        ProContent(
            state = ProUiState(loading = false, product = FakePurchases.CATALOG.pro),
            from = ProFeature.TAGS,
            onBuy = {},
            onRestore = {},
            onRetry = {},
            onClose = {}
        )
    }

    @Test
    @Config(qualifiers = "+h1000dp")
    fun proOffline() = compose.capture("pro_offline_$mode", dark = dark) {
        ProContent(
            state = ProUiState(loading = false),
            from = null,
            onBuy = {},
            onRestore = {},
            onRetry = {},
            onClose = {}
        )
    }

    @Test
    fun statsEmpty() = compose.capture("stats_empty_$mode", dark = dark) {
        StatsContent(
            state = StatsUiState(loading = false, today = DayProgress(LocalDate.now(), 0, 60)),
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
        compose.capture("todo_$mode", dark = dark) {
            val state by viewModel.uiState.collectAsState()
            TodoListContent(
                state = state,
                listState = rememberLazyListState(),
                viewModel = viewModel,
                onSeePro = {}
            )
        }
    }

    /** Con Pro a la venta y sin comprar: los temas y los iconos de Pro, con candado salvo el puesto. */
    @Test
    fun appearancePro() = compose.capture("appearance_pro_$mode", dark = dark) {
        AppearanceCard(
            appearance = Appearance(theme = AppTheme.Mint),
            isSupporter = false,
            onDarkMode = {},
            onDynamicColor = {},
            onTheme = {},
            pro = ProAccess.LOCKED,
            icon = AppIcon.MINT
        )
    }

    @Test
    fun support() = compose.capture("support_$mode", dark = dark) {
        SupportContent(
            state = SupportState(loading = false, tips = FakePurchases.CATALOG.tips),
            onTip = {},
            onRetry = {},
            onUseTheme = {},
            onClose = {}
        )
    }

    @Test
    fun supportThanks() = compose.capture("support_thanks_$mode", dark = dark) {
        SupportContent(
            state = SupportState(thanked = true),
            onTip = {},
            onRetry = {},
            onUseTheme = {},
            onClose = {}
        )
    }

    @Test
    fun soundPanel() = compose.capture("sound_panel_$mode", dark = dark) {
        Column(modifier = Modifier.padding(24.dp)) {
            ExpandedContent(
                items = PlayerType.entries.associateWith {
                    PlayerState(
                        volume = 0.5f,
                        isPlaying = it == PlayerType.RAIN || it == PlayerType.WHITE
                    )
                },
                onPlayClicked = { _, _ -> },
                onVolumeChanged = { _, _ -> }
            )
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "oscuro={0}")
        fun parameters() = listOf(arrayOf<Any>(false), arrayOf<Any>(true))
    }
}
