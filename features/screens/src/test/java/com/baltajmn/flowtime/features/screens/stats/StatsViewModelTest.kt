package com.baltajmn.flowtime.features.screens.stats

import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.stats.StatsSummary
import com.baltajmn.flowtime.data.stats.level
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.fakes.FakeDataProvider
import com.baltajmn.flowtime.features.screens.fakes.FakePurchases
import com.baltajmn.flowtime.features.screens.fakes.FakeSessions
import com.baltajmn.flowtime.features.screens.fakes.FakeStats
import com.baltajmn.flowtime.features.screens.fakes.FakeTags
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboard
import com.baltajmn.flowtime.features.screens.history.usecases.ImportMode
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboard
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

// Con Robolectric solo por el Context de DocumentFiles.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StatsViewModelTest {

    private val prefs = FakeDataProvider()
    private val sessions = FakeSessions()
    private val sep29 = LocalDate.of(2026, 9, 29)
    private val sep30 = LocalDate.of(2026, 9, 30)
    private val hour = StatsSummary(
        totalSeconds = 3600,
        sessions = 2,
        bestDay = sep29,
        bestDaySeconds = 3600
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        stats: FakeStats = FakeStats(),
        proEnabled: Boolean = false,
        pro: Boolean = false
    ) =
        StatsViewModel(
            stats = stats,
            goals = GoalRepository(prefs, sessions, days = flowOf(sep30)),
            tags = FakeTags("Estudio"),
            purchases = FakePurchases(pro = pro),
            files = DocumentFiles(RuntimeEnvironment.getApplication()),
            getAllStudyTime = GetAllStudyTime(sessions),
            getStudyTimeToClipboard = GetStudyTimeToClipboard(sessions),
            setStudyTimeFromClipboard = SetStudyTimeFromClipboard(sessions),
            proEnabled = proEnabled,
            today = { sep30 }
        )

    @Test
    fun `sin Pro a la venta lo de Pro ni se ve ni se consulta`() {
        val stats = FakeStats(hour)
        val viewModel = viewModel(stats)

        viewModel.load()

        assertEquals(ProAccess.HIDDEN, viewModel.uiState.value.pro)
        assertEquals(0, stats.proQueries)
    }

    @Test
    fun `sin Pro se consulta todo, para verlo difuminado con los datos reales`() {
        val stats = FakeStats(hour, byMode = mapOf("FLOW_TIME" to 600L, "POMODORO" to 3000L))
        val viewModel = viewModel(stats, proEnabled = true)

        viewModel.load()

        val state = viewModel.uiState.value
        assertEquals(ProAccess.LOCKED, state.pro)
        assertEquals(
            listOf(TimerMode.POMODORO to 3000L, TimerMode.FLOW_TIME to 600L),
            state.details.byMode
        )
    }

    @Test
    fun `con Pro todo abierto`() {
        val viewModel = viewModel(FakeStats(hour), proEnabled = true, pro = true)

        viewModel.load()

        assertEquals(ProAccess.OPEN, viewModel.uiState.value.pro)
    }

    @Test
    fun `un periodo sin nada no consulta lo de Pro`() {
        val stats = FakeStats()
        val viewModel = viewModel(stats, proEnabled = true, pro = true)

        viewModel.load()

        assertEquals(0, stats.proQueries)
    }

    @Test
    fun `hacia delante no pasa del periodo en curso`() {
        val viewModel = viewModel()
        viewModel.nextPeriod()
        assertEquals(0, viewModel.uiState.value.period.offset)

        viewModel.previousPeriod()
        assertEquals(1, viewModel.uiState.value.period.offset)

        viewModel.nextPeriod()
        assertEquals(0, viewModel.uiState.value.period.offset)
    }

    @Test
    fun `sin ninguna sesion ensena el estado vacio, y con alguna el nivel`() {
        val viewModel = viewModel()
        viewModel.load()
        assertFalse(viewModel.uiState.value.loading)
        assertFalse(viewModel.uiState.value.hasSessions)

        sessions.days[sep29] = 100 * 3600L
        viewModel.load()

        assertTrue(viewModel.uiState.value.hasSessions)
        assertEquals(level(6000), viewModel.uiState.value.level)
    }

    @Test
    fun `sin dias con datos importa directamente y resume el resultado`() {
        val viewModel = viewModel()

        viewModel.importStudyTime("29092026: 45\ntheme_color: 1")

        assertEquals(mapOf(sep29 to 45 * 60L), sessions.days)
        assertEquals(
            StatsMessage.Imported(days = 1, ignoredLines = 1),
            viewModel.uiState.value.message
        )
        // Y la pantalla ya enseña lo importado.
        assertTrue(viewModel.uiState.value.hasSessions)
    }

    @Test
    fun `con dias con datos pregunta antes de escribir`() {
        sessions.days[sep29] = 60 * 60L
        val viewModel = viewModel()

        viewModel.importStudyTime("29092026: 30\n30092026: 10")

        assertEquals(1, viewModel.uiState.value.pendingImport?.daysWithData)
        assertEquals(mapOf(sep29 to 60 * 60L), sessions.days)
    }

    @Test
    fun `al elegir sumar se suman los minutos`() {
        sessions.days[sep29] = 60 * 60L
        val viewModel = viewModel()
        viewModel.importStudyTime("29092026: 30")

        viewModel.resolvePendingImport(ImportMode.SUM)

        assertEquals(90 * 60L, sessions.days[sep29])
        assertNull(viewModel.uiState.value.pendingImport)
    }

    @Test
    fun `al cancelar no se escribe nada`() {
        sessions.days[sep29] = 60 * 60L
        val viewModel = viewModel()
        viewModel.importStudyTime("29092026: 30")

        viewModel.resolvePendingImport(null)

        assertEquals(60 * 60L, sessions.days[sep29])
        assertNull(viewModel.uiState.value.message)
    }

    @Test
    fun `un texto sin dias validos avisa de que no hay nada que importar`() {
        val viewModel = viewModel()

        viewModel.importStudyTime("hola")

        assertEquals(
            StatsMessage.Imported(days = 0, ignoredLines = 1),
            viewModel.uiState.value.message
        )
    }

    @Test
    fun `exporta los minutos de cada dia sacados de las sesiones`() {
        sessions.days[sep30] = 10 * 60L + 59
        sessions.days[sep29] = 45 * 60L

        var text = ""
        viewModel().exportStudyTime { text = it }

        assertEquals("29092026: 45\n30092026: 10", text)
    }
}
