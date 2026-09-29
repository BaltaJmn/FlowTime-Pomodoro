package com.baltajmn.flowtime.features.screens.history

import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
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
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val sessions = FakeSessions()
    private val sep29 = LocalDate.of(2026, 9, 29)
    private val sep30 = LocalDate.of(2026, 9, 30)

    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = HistoryViewModel(
            getStudyTime = GetStudyTime(sessions),
            getAllStudyTimeUseCase = GetAllStudyTime(sessions),
            getStudyTimeToClipboard = GetStudyTimeToClipboard(sessions),
            setStudyTimeFromClipboard = SetStudyTimeFromClipboard(sessions)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sin dias con datos importa directamente y resume el resultado`() {
        viewModel.importStudyTime("29092026: 45\ntheme_color: 1")

        assertEquals(mapOf(sep29 to 45 * 60L), sessions.days)
        assertEquals(
            ImportSummary(importedDays = 1, ignoredLines = 1),
            viewModel.uiState.value.importSummary
        )
    }

    @Test
    fun `con dias con datos pregunta antes de escribir`() {
        sessions.days[sep29] = 60 * 60L

        viewModel.importStudyTime("29092026: 30\n30092026: 10")

        assertEquals(1, viewModel.uiState.value.pendingImport?.daysWithData)
        assertEquals(mapOf(sep29 to 60 * 60L), sessions.days)
    }

    @Test
    fun `al elegir sumar se suman los minutos`() {
        sessions.days[sep29] = 60 * 60L
        viewModel.importStudyTime("29092026: 30")

        viewModel.resolvePendingImport(ImportMode.SUM)

        assertEquals(90 * 60L, sessions.days[sep29])
        assertNull(viewModel.uiState.value.pendingImport)
    }

    @Test
    fun `al cancelar no se escribe nada`() {
        sessions.days[sep29] = 60 * 60L
        viewModel.importStudyTime("29092026: 30")

        viewModel.resolvePendingImport(null)

        assertEquals(60 * 60L, sessions.days[sep29])
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

    @Test
    fun `exporta los minutos de cada dia sacados de las sesiones`() {
        sessions.days[sep30] = 10 * 60L + 59
        sessions.days[sep29] = 45 * 60L

        var text = ""
        viewModel.exportStudyTime { text = it }

        assertEquals("29092026: 45\n30092026: 10", text)
    }
}
