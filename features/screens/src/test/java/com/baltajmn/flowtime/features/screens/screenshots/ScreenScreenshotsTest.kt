package com.baltajmn.flowtime.features.screens.screenshots

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.baltajmn.flowtime.core.design.components.CurrentlyPlaying
import com.baltajmn.flowtime.core.design.components.ExpandedContent
import com.baltajmn.flowtime.core.design.components.TopBarSurface
import com.baltajmn.flowtime.core.design.sound.PlayerState
import com.baltajmn.flowtime.core.design.sound.PlayerType
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.screen.TimerLandscapeContent
import com.baltajmn.flowtime.features.screens.fakes.FakeBackups
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import com.baltajmn.flowtime.features.screens.fakes.FakeTags
import com.baltajmn.flowtime.features.screens.fakes.FakeTasks
import com.baltajmn.flowtime.features.screens.history.HistoryContent
import com.baltajmn.flowtime.features.screens.history.HistoryViewModel
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboard
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboard
import com.baltajmn.flowtime.features.screens.home.HomeContent
import com.baltajmn.flowtime.features.screens.settings.SettingsContent
import com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
import com.baltajmn.flowtime.features.screens.timer.TimerUiState
import com.baltajmn.flowtime.features.screens.todoList.TodoListContent
import com.baltajmn.flowtime.features.screens.todoList.TodoListViewModel
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.RuntimeEnvironment
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

    /** Una semana con algo de todo, para que el historial y el nivel no salgan vacíos. */
    private fun sessions() = FakeSessions().apply {
        val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        listOf(95L, 40L, 130L, 0L, 75L, 20L, 0L).forEachIndexed { day, minutes ->
            days[monday.plusDays(day.toLong())] = minutes * 60
        }
        days[monday.minusWeeks(3)] = 40 * 60 * 60L
    }

    @Test
    fun home() = compose.capture("home_$mode", dark = dark) {
        HomeContent(navigateToScreen = {})
    }

    @Test
    fun timerLandscape() = compose.capture("timer_landscape_$mode", dark = dark) {
        TimerLandscapeContent(
            state = TimerUiState(
                mode = TimerMode.POMODORO,
                phase = Phase.BREAK,
                time = "04:12",
                minutesToday = "45 min"
            ),
            title = "Resting",
            onAction = {},
            onSwitchChanged = {}
        )
    }

    @Test
    fun settings() {
        val prefs = FakeDataProvider()
        val sessions = sessions()
        val viewModel = SettingsViewModel(
            prefs,
            GetAllStudyTime(sessions),
            AppearanceRepository(prefs),
            FakeBackups(lastExportAt = System.currentTimeMillis()),
            DocumentFiles(RuntimeEnvironment.getApplication()),
            GoalRepository(prefs, sessions, days = flowOf(LocalDate.now())),
            FakeTags("Estudio", "Trabajo", "Lectura", "Casa")
        )
        compose.capture("settings_$mode", dark = dark) {
            val state by viewModel.uiState.collectAsState()
            SettingsContent(
                state = state,
                listState = rememberLazyListState(),
                viewModel = viewModel,
                showSound = true,
                onSoundChange = {},
                navigateToHistory = {},
                onSupportDeveloperClick = {}
            )
        }
    }

    @Test
    fun history() {
        val sessions = sessions()
        val viewModel = HistoryViewModel(
            GetStudyTime(sessions),
            GetAllStudyTime(sessions),
            GetStudyTimeToClipboard(sessions),
            SetStudyTimeFromClipboard(sessions)
        )
        compose.capture("history_$mode", dark = dark) {
            val state by viewModel.uiState.collectAsState()
            HistoryContent(state = state, viewModel = viewModel, navigateUp = {})
        }
    }

    @Test
    fun todoList() {
        val today = LocalDate.now()
        val tasks = FakeTasks(
            Task(1, "Repasar el tema 4", "Apuntes y ejercicios del final", plannedFor = today),
            Task(2, "Leer 20 páginas", plannedFor = today, doneOn = today, position = 1),
            Task(
                id = 3,
                title = "Preparar la presentación",
                description = "Diapositivas 5 a 12",
                plannedFor = today.minusDays(1)
            )
        )
        val viewModel = TodoListViewModel(tasks, today = { today })
        compose.capture("todo_$mode", dark = dark) {
            val state by viewModel.uiState.collectAsState()
            TodoListContent(
                state = state,
                listState = rememberLazyListState(),
                viewModel = viewModel
            )
        }
    }

    @Test
    fun soundPanel() = compose.capture("sound_panel_$mode", dark = dark) {
        TopBarSurface(expanded = true) {
            CurrentlyPlaying(onExpandedClick = {}, rotationState = 180f, hasActivePlayers = true)
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
