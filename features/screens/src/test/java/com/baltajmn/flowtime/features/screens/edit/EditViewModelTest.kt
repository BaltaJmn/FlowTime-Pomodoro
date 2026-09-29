package com.baltajmn.flowtime.features.screens.edit

import androidx.lifecycle.SavedStateHandle
import com.baltajmn.flowtime.core.common.dispatchers.TestDispatcherProvider
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.core.persistence.model.TimerDefaults
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FLOW_TIME_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PERCENTAGE_RANGE
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.POMODORO_RANGE
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class EditViewModelTest {

    private val dataProvider = FakeDataProvider()

    private fun viewModel(type: ScreenType) = EditViewModel(
        savedStateHandle = SavedStateHandle(mapOf("type" to type.name)),
        dispatcherProvider = TestDispatcherProvider(),
        dataProvider = dataProvider
    )

    @Test
    fun `muestra la configuracion guardada de cada modo`() {
        val savedRanges = listOf(
            RangeModel(totalRange = 20, endRange = 20, rest = 5),
            RangeModel(totalRange = 50, endRange = 30, rest = 25)
        )
        dataProvider.setObject(FLOW_TIME_RANGE, savedRanges)
        dataProvider.setObject(POMODORO_RANGE, RangeModel(totalRange = 25, endRange = 25, rest = 5))
        dataProvider.setLong(PERCENTAGE_RANGE, 35)

        val state = viewModel(ScreenType.FlowTime).uiState.value

        assertEquals(savedRanges, state.flowTimeRanges)
        assertEquals(RangeModel(totalRange = 25, endRange = 25, rest = 5), state.pomodoroRange)
        assertEquals(35L, state.percentage)
    }

    @Test
    fun `sin nada guardado muestra lo mismo que usa el temporizador`() {
        val state = viewModel(ScreenType.Pomodoro).uiState.value

        assertEquals(TimerDefaults.pomodoro(), state.pomodoroRange)
        assertEquals(TimerDefaults.flowTimeRanges(), state.flowTimeRanges)
        assertEquals(TimerDefaults.PERCENTAGE, state.percentage)
    }

    @Test
    fun `guardar Pomodoro no toca FlowTime ni Porcentaje`() {
        val viewModel = viewModel(ScreenType.Pomodoro)

        viewModel.modifyPomodoro(RangeModel(totalRange = 30, endRange = 30, rest = 10))
        viewModel.saveChanges()

        assertEquals(
            RangeModel(totalRange = 30, endRange = 30, rest = 10),
            dataProvider.getRangeModel(POMODORO_RANGE)
        )
        assertFalse(dataProvider.values.containsKey(FLOW_TIME_RANGE.name.lowercase()))
        assertFalse(dataProvider.values.containsKey(PERCENTAGE_RANGE.name.lowercase()))
    }

    @Test
    fun `guardar Porcentaje no toca los otros modos`() {
        val viewModel = viewModel(ScreenType.Percentage)

        viewModel.modifyPercentage(40)
        viewModel.saveChanges()

        assertEquals(40L, dataProvider.getLong(PERCENTAGE_RANGE))
        assertFalse(dataProvider.values.containsKey(POMODORO_RANGE.name.lowercase()))
        assertFalse(dataProvider.values.containsKey(FLOW_TIME_RANGE.name.lowercase()))
    }

    @Test
    fun `un porcentaje guardado a 0 pasa al valor por defecto`() {
        dataProvider.setLong(PERCENTAGE_RANGE, 0)

        val state = viewModel(ScreenType.Percentage).uiState.value

        assertEquals(TimerDefaults.PERCENTAGE, state.percentage)
    }

    @Test
    fun `borrar un rango intermedio recalcula los limites de los siguientes`() {
        val viewModel = viewModel(ScreenType.FlowTime)
        viewModel.addRange()

        viewModel.deleteRange(1)

        val totals = viewModel.uiState.value.flowTimeRanges.map { it.totalRange }
        assertEquals(listOf(15, 30, 45), totals)
    }

    @Test
    fun `cambiar la duracion de un rango mueve los limites de los siguientes`() {
        val viewModel = viewModel(ScreenType.FlowTime)

        viewModel.modifyRange(0, RangeModel(totalRange = 20, endRange = 20, rest = 5))

        val totals = viewModel.uiState.value.flowTimeRanges.map { it.totalRange }
        assertEquals(listOf(20, 35, 50), totals)
    }

    @Test
    fun `el porcentaje queda entre el minimo y el maximo`() {
        assertEquals(TimerDefaults.PERCENTAGE, TimerDefaults.percentage(0))
        assertEquals(TimerDefaults.PERCENTAGE, TimerDefaults.percentage(-10))
        assertEquals(5L, TimerDefaults.percentage(5))
        assertEquals(33L, TimerDefaults.percentage(33))
        assertEquals(100L, TimerDefaults.percentage(250))
    }
}
