package com.baltajmn.flowtime.features.screens.onboard

import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ON_BOARD
import com.baltajmn.flowtime.data.goal.DailyGoal
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.TimeSource
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.Back
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.NavigateToMainGraph
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnBoardViewModelTest {

    private object StillTime : TimeSource {
        override fun wallMillis() = 1_790_000_000_000L
        override fun elapsedMillis() = 3_600_000L
        override fun bootCount() = 7
    }

    private val prefs = FakeDataProvider()
    private val sessions = FakeSessions()
    private val engine = FocusEngine(prefs, StillTime, sessions)
    private val goals = GoalRepository(prefs, sessions, days = flowOf(LocalDate.of(2026, 9, 29)))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = OnBoardViewModel(prefs, engine, goals)

    @Test
    fun `la primera vez propone FlowTime y el objetivo por defecto`() {
        val state = viewModel().uiState.value

        assertEquals(TimerMode.FLOW_TIME, state.mode)
        assertEquals(DailyGoal.DEFAULT_MINUTES, state.goalMinutes)
    }

    @Test
    fun `empezar guarda el modo y el objetivo y abre ese temporizador`() = runTest {
        val viewModel = viewModel()
        viewModel.selectMode(TimerMode.PERCENTAGE)
        viewModel.changeGoal(DailyGoal.STEP_MINUTES)
        viewModel.changeGoal(DailyGoal.STEP_MINUTES)

        viewModel.start()

        assertEquals(NavigateToMainGraph, viewModel.event.first())
        assertEquals(TimerMode.PERCENTAGE, engine.state.value.mode)
        assertEquals(DailyGoal.DEFAULT_MINUTES + 10, goals.currentGoal)
        assertFalse(prefs.getCheckValue(SHOW_ON_BOARD))
    }

    @Test
    fun `saltar termina con FlowTime y el objetivo por defecto, sin abrir el temporizador`() = runTest {
        val viewModel = viewModel()
        viewModel.selectMode(TimerMode.POMODORO)
        viewModel.changeGoal(-DailyGoal.STEP_MINUTES)

        viewModel.skip()

        assertEquals(NavigateToMainGraph, viewModel.event.first())
        assertEquals(TimerMode.FLOW_TIME, engine.state.value.mode)
        assertEquals(DailyGoal.DEFAULT_MINUTES, goals.currentGoal)
        assertFalse(prefs.getCheckValue(SHOW_ON_BOARD))
    }

    @Test
    fun `desde Ajustes parte de lo que hay, y saltar no cambia nada y vuelve`() = runTest {
        prefs.setCheckValue(SHOW_ON_BOARD, false)
        engine.select(TimerMode.PERCENTAGE)
        goals.setGoal(90)
        val viewModel = viewModel()

        assertEquals(TimerMode.PERCENTAGE, viewModel.uiState.value.mode)
        assertEquals(90, viewModel.uiState.value.goalMinutes)

        viewModel.selectMode(TimerMode.POMODORO)
        viewModel.skip()

        assertEquals(Back, viewModel.event.first())
        assertEquals(TimerMode.PERCENTAGE, engine.state.value.mode)
        assertEquals(90, goals.currentGoal)
    }

    @Test
    fun `desde Ajustes, empezar tambien vuelve`() = runTest {
        prefs.setCheckValue(SHOW_ON_BOARD, false)
        val viewModel = viewModel()
        viewModel.selectMode(TimerMode.FLOW_TIME)

        viewModel.start()

        assertEquals(Back, viewModel.event.first())
        assertEquals(TimerMode.FLOW_TIME, engine.state.value.mode)
    }

    @Test
    fun `el objetivo no sale de sus limites`() {
        val viewModel = viewModel()

        repeat(200) { viewModel.changeGoal(-DailyGoal.STEP_MINUTES) }
        assertEquals(DailyGoal.MIN_MINUTES, viewModel.uiState.value.goalMinutes)

        repeat(200) { viewModel.changeGoal(DailyGoal.STEP_MINUTES) }
        assertEquals(DailyGoal.MAX_MINUTES, viewModel.uiState.value.goalMinutes)
    }

    @Test
    fun `con una sesion en marcha el modo no se cambia`() {
        prefs.setCheckValue(SHOW_ON_BOARD, false)
        engine.start(TimerMode.POMODORO)
        val viewModel = viewModel()

        viewModel.selectMode(TimerMode.FLOW_TIME)

        assertTrue(viewModel.uiState.value.modeLocked)
        assertEquals(TimerMode.POMODORO, viewModel.uiState.value.mode)
    }
}
