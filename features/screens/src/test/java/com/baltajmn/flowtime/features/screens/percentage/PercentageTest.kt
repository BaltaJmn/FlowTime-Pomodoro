package com.baltajmn.flowtime.features.screens.percentage

import com.baltajmn.flowtime.core.common.dispatchers.DispatcherProvider
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import io.mockk.mockk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PercentageTest {

    private class SingleDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
        override val io = dispatcher
        override val mainImmediate = dispatcher
        override val default = dispatcher
    }

    private fun TestScope.viewModel() = PercentageViewModel(
        dispatcherProvider = SingleDispatcherProvider(StandardTestDispatcher(testScheduler)),
        dataProvider = FakeDataProvider(),
        soundService = mockk<SoundService>(relaxed = true)
    ).apply { getPercentageConfig() }

    private fun TestScope.advanceSeconds(seconds: Long) {
        advanceTimeBy(seconds * 1000)
        runCurrent()
    }

    @Test
    fun `el descanso es el porcentaje de lo trabajado`() {
        assertEquals(10L, percentageBreakSeconds(workedSeconds = 50, percentage = 20))
        assertEquals(1188L, percentageBreakSeconds(workedSeconds = 3600, percentage = 33))
    }

    @Test
    fun `el descanso se redondea por abajo al segundo`() {
        assertEquals(11L, percentageBreakSeconds(workedSeconds = 59, percentage = 20))
    }

    @Test
    fun `el contador avanza un segundo por segundo`() = runTest {
        val viewModel = viewModel()

        viewModel.startTimer()
        advanceSeconds(10)

        assertEquals(10L, viewModel.uiState.value.seconds)
        viewModel.stopTimer()
    }

    // Reseña de noviembre de 2024: el contador del modo Porcentaje corría demasiado rápido.
    @Test
    fun `empezar dos veces no acelera el contador`() = runTest {
        val viewModel = viewModel()

        viewModel.startTimer()
        viewModel.startTimer()
        advanceSeconds(10)

        assertEquals(10L, viewModel.uiState.value.seconds)
        viewModel.stopTimer()
    }

    @Test
    fun `el descanso dura el porcentaje configurado y despues se vuelve a trabajar`() = runTest {
        val viewModel = viewModel()
        viewModel.startTimer()
        advanceSeconds(50)

        viewModel.continueWithBreak()
        runCurrent()

        assertTrue(viewModel.uiState.value.isBreakRunning)
        assertEquals(10L, viewModel.uiState.value.secondsBreak)

        advanceSeconds(10)

        assertFalse(viewModel.uiState.value.isBreakRunning)
        assertTrue(viewModel.uiState.value.isTimerRunning)
        viewModel.stopTimer()
    }
}
