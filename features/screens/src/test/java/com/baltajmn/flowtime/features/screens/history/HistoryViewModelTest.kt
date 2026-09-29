package com.baltajmn.flowtime.features.screens.history

import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboard
import com.baltajmn.flowtime.features.screens.history.usecases.ImportMode
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val dataProvider = FakeDataProvider()

    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = HistoryViewModel(
            getStudyTime = GetStudyTime(dataProvider),
            getAllStudyTimeUseCase = GetAllStudyTime(dataProvider),
            getStudyTimeToClipboard = GetStudyTimeToClipboard(dataProvider),
            setStudyTimeFromClipboard = SetStudyTimeFromClipboard(dataProvider)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin dias con datos importa directamente y resume el resultado`() {
        viewModel.importStudyTime("29092026: 45\ntheme_color: 1")

        assertEquals(45L, dataProvider.values["29092026"])
        assertNull(dataProvider.values["theme_color"])
        assertEquals(
            ImportSummary(importedDays = 1, ignoredLines = 1),
            viewModel.uiState.value.importSummary
        )
    }

    @Test
    fun `con dias con datos pregunta antes de escribir`() {
        dataProvider.values["29092026"] = 60L

        viewModel.importStudyTime("29092026: 30\n30092026: 10")

        assertEquals(1, viewModel.uiState.value.pendingImport?.daysWithData)
        assertEquals(60L, dataProvider.values["29092026"])
        assertNull(dataProvider.values["30092026"])
    }

    @Test
    fun `al elegir sumar se suman los minutos`() {
        dataProvider.values["29092026"] = 60L
        viewModel.importStudyTime("29092026: 30")

        viewModel.resolvePendingImport(ImportMode.SUM)

        assertEquals(90L, dataProvider.values["29092026"])
        assertNull(viewModel.uiState.value.pendingImport)
    }

    @Test
    fun `al cancelar no se escribe nada`() {
        dataProvider.values["29092026"] = 60L
        viewModel.importStudyTime("29092026: 30")

        viewModel.resolvePendingImport(null)

        assertEquals(60L, dataProvider.values["29092026"])
        assertNull(viewModel.uiState.value.importSummary)
    }

    @Test
    fun `un texto sin dias validos avisa de que no hay nada que importar`() {
        viewModel.importStudyTime("hola")

        assertEquals(
            ImportSummary(importedDays = 0, ignoredLines = 1),
            viewModel.uiState.value.importSummary
        )
    }
}
