package com.baltajmn.flowtime.features.screens.screenshots

import org.robolectric.RuntimeEnvironment
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.goal.DayProgress
import com.baltajmn.flowtime.data.goal.GoalRepository
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
import com.baltajmn.flowtime.data.timer.DoNotDisturb
import com.baltajmn.flowtime.data.timer.FocusMode
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeBackups
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakePurchases
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import com.baltajmn.flowtime.features.screens.fakes.FakeTags
import com.baltajmn.flowtime.features.screens.fakes.FakeTasks
import com.baltajmn.flowtime.features.screens.focus.FocusUiState
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import com.baltajmn.flowtime.features.screens.settings.AppIcons
import com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
import com.baltajmn.flowtime.features.screens.stats.StatsDetails
import com.baltajmn.flowtime.features.screens.stats.StatsUiState
import com.baltajmn.flowtime.features.screens.todoList.TodoListViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

/* Los datos de ejemplo de las capturas, los mismos en todos los tamaños. */

/** Una semana con algo de todo, para que el historial y el nivel no salgan vacíos. */
internal fun sampleSessions() = FakeSessions().apply {
    val monday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    listOf(95L, 40L, 130L, 0L, 75L, 20L, 0L).forEachIndexed { day, minutes ->
        days[monday.plusDays(day.toLong())] = minutes * 60
    }
    days[monday.minusWeeks(3)] = 40 * 60 * 60L
}

internal val sampleFocus = FocusUiState(
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
)

/** Un mes con algo de todo; [pro] decide si lo de Pro se ve abierto o difuminado. */
internal fun sampleStats(pro: ProAccess): StatsUiState {
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

internal fun sampleSettings(): SettingsViewModel {
    val prefs = FakeDataProvider()
    return SettingsViewModel(
        prefs,
        AppearanceRepository(prefs),
        FakeBackups(lastExportAt = System.currentTimeMillis()),
        DocumentFiles(RuntimeEnvironment.getApplication()),
        GoalRepository(prefs, sampleSessions(), days = flowOf(LocalDate.now())),
        FakeTags("Estudio", "Trabajo", "Lectura", "Casa"),
        FakePurchases(supporter = true),
        ReminderRepository(prefs).apply { set(Reminder(enabled = true)) },
        FocusMode(prefs, NoDoNotDisturb, MutableStateFlow(false)),
        AppIcons(RuntimeEnvironment.getApplication())
    )
}

internal fun sampleTodo(): TodoListViewModel {
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
    return TodoListViewModel(tasks, today = { today })
}

private object NoDoNotDisturb : DoNotDisturb {
    override val granted = false
    override val hasRules = true
    override var filter = 0
    override fun setRule(active: Boolean) = Unit
}
