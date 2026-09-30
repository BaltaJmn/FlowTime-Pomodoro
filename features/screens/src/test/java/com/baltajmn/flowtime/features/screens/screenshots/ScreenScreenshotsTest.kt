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
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.reminder.Reminder
import com.baltajmn.flowtime.data.reminder.ReminderRepository
import com.baltajmn.flowtime.data.stats.PeriodKind
import com.baltajmn.flowtime.data.stats.StatsPeriod
import com.baltajmn.flowtime.data.stats.StatsSummary
import com.baltajmn.flowtime.data.stats.TaskTime
import com.baltajmn.flowtime.data.stats.level
import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeBackups
import com.baltajmn.flowtime.features.screens.focus.FocusContent
import com.baltajmn.flowtime.features.screens.focus.FocusUiState
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakePurchases
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import com.baltajmn.flowtime.features.screens.fakes.FakeTags
import com.baltajmn.flowtime.features.screens.fakes.FakeTasks
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import com.baltajmn.flowtime.features.screens.pro.ProContent
import com.baltajmn.flowtime.features.screens.pro.ProFeature
import com.baltajmn.flowtime.features.screens.pro.ProUiState
import com.baltajmn.flowtime.features.screens.stats.StatsContent
import com.baltajmn.flowtime.features.screens.stats.StatsDetails
import com.baltajmn.flowtime.features.screens.stats.StatsUiState
import com.baltajmn.flowtime.features.screens.settings.SettingsContent
import com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
import com.baltajmn.flowtime.features.screens.support.SupportContent
import com.baltajmn.flowtime.features.screens.support.SupportState
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
    fun focus() = compose.capture("focus_$mode", dark = dark) {
        FocusContent(
            state = FocusUiState(
                mode = TimerMode.FLOW_TIME,
                time = "00:00",
                minutesToday = "45 min",
                goalToday = "1 h",
                streak = 3,
                tags = listOf("Estudio", "Trabajo", "Lectura", "Casa").mapIndexed { i, name ->
                    Tag(
                        i + 1L,
                        name,
                        i
                    )
                },
                tagId = 1
            ),
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
        val prefs = FakeDataProvider()
        val sessions = sessions()
        val viewModel = SettingsViewModel(
            prefs,
            AppearanceRepository(prefs),
            FakeBackups(lastExportAt = System.currentTimeMillis()),
            DocumentFiles(RuntimeEnvironment.getApplication()),
            GoalRepository(prefs, sessions, days = flowOf(LocalDate.now())),
            FakeTags("Estudio", "Trabajo", "Lectura", "Casa"),
            FakePurchases(supporter = true),
            ReminderRepository(prefs).apply { set(Reminder(enabled = true)) }
        )
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

    /** Un mes con algo de todo; [pro] decide si lo de Pro se ve abierto o difuminado. */
    private fun statsState(pro: ProAccess): StatsUiState {
        val today = LocalDate.now()
        val period = StatsPeriod(PeriodKind.MONTH)
        val range = period.range(today)
        val minutes = listOf(95L, 40L, 130L, 0L, 75L, 20L, 0L, 60L, 45L, 110L)
        return StatsUiState(
            loading = false,
            hasSessions = true,
            level = level(totalMinutes = 3000),
            today = DayProgress(today, seconds = 35 * 60L, goalMinutes = 60),
            streak = Streak(current = 4, best = 9),
            period = period,
            range = range,
            summary = StatsSummary(
                totalSeconds = 10 * 3600L,
                sessions = 14,
                averageSessionSeconds = 43 * 60L,
                dailyAverageSeconds = 60 * 60L,
                bestDay = range.start.plusDays(2),
                bestDaySeconds = 130 * 60L,
                byDay = minutes.mapIndexed { day, it -> range.start.plusDays(day.toLong()) to it * 60 }.toMap()
            ),
            pro = pro,
            details = StatsDetails(
                change = 0.12f,
                byHour = List(24) { hour -> if (hour in 8..22) ((hour * 37) % 11) * 600L else 0L },
                byMode = listOf(TimerMode.POMODORO to 6 * 3600L, TimerMode.FLOW_TIME to 4 * 3600L),
                byTag = listOf(1L to 5 * 3600L, 2L to 3 * 3600L, null to 2 * 3600L),
                topTasks = listOf(
                    TaskTime(1, "Repasar el tema 4", 3 * 3600L),
                    TaskTime(2, "Leer 20 páginas", 5400L)
                )
            ),
            tags = listOf(Tag(1, "Estudio", 0), Tag(2, "Trabajo", 1))
        )
    }

    @Test
    @Config(qualifiers = "+h1700dp")
    fun stats() = compose.capture("stats_$mode", dark = dark) {
        StatsContent(
            state = statsState(ProAccess.OPEN),
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
            state = statsState(ProAccess.LOCKED).copy(
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
                viewModel = viewModel,
                onSeePro = {}
            )
        }
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
