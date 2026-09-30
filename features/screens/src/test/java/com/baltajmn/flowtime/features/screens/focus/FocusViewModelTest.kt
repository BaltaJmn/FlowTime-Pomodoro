package com.baltajmn.flowtime.features.screens.focus

import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimeSource
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import com.baltajmn.flowtime.features.screens.fakes.FakeTags
import com.baltajmn.flowtime.features.screens.fakes.FakeTasks
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FocusViewModelTest {

    private object StillTime : TimeSource {
        override fun wallMillis() = 1_790_000_000_000L
        override fun elapsedMillis() = 3_600_000L
        override fun bootCount() = 7
    }

    private val prefs = FakeDataProvider()
    private val sessions = FakeSessions()
    private val engine = FocusEngine(prefs, StillTime, sessions)
    private val goals = GoalRepository(prefs, sessions, days = flowOf(LocalDate.of(2026, 9, 30)))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Con alguien mirando la pantalla, como en la app: si no, el estado no se calcula. */
    private fun TestScope.viewModel() = FocusViewModel(
        engine,
        prefs,
        goals,
        FakeTags(),
        FakeTasks()
    ).also { vm ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }
    }

    @Test
    fun `sin sesion, elegir un modo lo cambia en el motor y en la pantalla`() = runTest {
        val viewModel = viewModel()

        viewModel.select(TimerMode.POMODORO)

        assertEquals(TimerMode.POMODORO, engine.state.value.mode)
        assertEquals(TimerMode.POMODORO, viewModel.uiState.value.mode)
        // Parada, enseña el tiempo de trabajo del modo, con el anillo lleno de la cuenta atrás.
        assertEquals("45:00", viewModel.uiState.value.time)
        assertEquals(1f, viewModel.uiState.value.progress)
    }

    @Test
    fun `con una sesion en marcha no deja cambiar de modo`() = runTest {
        engine.start(TimerMode.PERCENTAGE)
        val viewModel = viewModel()

        viewModel.select(TimerMode.POMODORO)

        assertEquals(TimerMode.PERCENTAGE, engine.state.value.mode)
        assertEquals(TimerMode.PERCENTAGE, viewModel.uiState.value.mode)
    }

    @Test
    fun `al abrirla desde la notificacion ensena el modo y la fase de la sesion`() = runTest {
        engine.start(TimerMode.POMODORO)

        val state = viewModel().uiState.value

        assertEquals(TimerMode.POMODORO, state.mode)
        assertEquals(Phase.WORK, state.phase)
        assertTrue(state.isActive)
    }

    @Test
    fun `empezar arranca el modo elegido`() = runTest {
        val viewModel = viewModel()
        viewModel.select(TimerMode.POMODORO)

        viewModel.onAction(TimerAction.START)

        assertEquals(TimerMode.POMODORO, engine.state.value.mode)
        assertEquals(Phase.WORK, engine.state.value.phase)
    }
}
