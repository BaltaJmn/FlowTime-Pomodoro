package com.baltajmn.flowtime.data.review

import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.fakes.FakeSessionRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {

    private val day = TimeUnit.DAYS.toMillis(1)
    private val installed = 1_000_000_000_000L
    private var now = installed + 10 * day
    private var running = false
    private val sessions = FakeSessionRepository().apply { sessionCount = 3 }
    private val policy = ReviewPolicy(
        dataProvider = FakeDataProvider(),
        sessions = sessions,
        sessionRunning = { running },
        installedAt = { installed },
        clock = { now }
    )

    @Test
    fun `con 2 sesiones no pide y con 3 si`() = runTest {
        sessions.sessionCount = 2
        assertFalse(policy.shouldAsk())

        sessions.sessionCount = 3
        assertTrue(policy.shouldAsk())
    }

    @Test
    fun `los dos primeros dias tras instalar no pide`() = runTest {
        now = installed + day
        assertFalse(policy.shouldAsk())

        now = installed + 2 * day
        assertFalse(policy.shouldAsk())

        now = installed + 2 * day + 1
        assertTrue(policy.shouldAsk())
    }

    @Test
    fun `pide una vez y no vuelve a pedir en 120 dias`() = runTest {
        assertTrue(policy.shouldAsk())
        policy.asked()

        now += 119 * day
        assertFalse(policy.shouldAsk())

        now += 2 * day
        assertTrue(policy.shouldAsk())
    }

    @Test
    fun `con una sesion en marcha no pide`() = runTest {
        running = true

        assertFalse(policy.shouldAsk())
    }

    private val sessionRunning = MutableStateFlow(false)
    private val celebrating = MutableStateFlow(false)

    @Test
    fun `lo que hay al abrir la app no es un momento para pedirla`() = runTest {
        val moments = mutableListOf<Unit>()
        backgroundScope.launch { calmMoments(sessionRunning, celebrating).collect(moments::add) }

        advanceTimeBy(60_000)

        assertEquals(0, moments.size)
    }

    @Test
    fun `al terminar una sesion, con un margen`() = runTest {
        sessionRunning.value = true
        val moments = mutableListOf<Unit>()
        backgroundScope.launch { calmMoments(sessionRunning, celebrating).collect(moments::add) }
        runCurrent()

        sessionRunning.value = false
        advanceTimeBy(SETTLE_MILLIS - 1)
        assertEquals(0, moments.size)

        advanceTimeBy(2)
        assertEquals(1, moments.size)
    }

    @Test
    fun `si llega la celebracion, se espera a que se cierre`() = runTest {
        sessionRunning.value = true
        val moments = mutableListOf<Unit>()
        backgroundScope.launch { calmMoments(sessionRunning, celebrating).collect(moments::add) }
        runCurrent()

        sessionRunning.value = false
        advanceTimeBy(500)
        celebrating.value = true
        advanceTimeBy(60_000)
        assertEquals(0, moments.size)

        celebrating.value = false
        advanceTimeBy(SETTLE_MILLIS + 1)
        assertEquals(1, moments.size)
    }

    @Test
    fun `cerrar la celebracion en el descanso no cuenta, terminar despues si`() = runTest {
        sessionRunning.value = true
        celebrating.value = true
        val moments = mutableListOf<Unit>()
        backgroundScope.launch { calmMoments(sessionRunning, celebrating).collect(moments::add) }
        runCurrent()

        celebrating.value = false
        advanceTimeBy(60_000)
        assertEquals(0, moments.size)

        sessionRunning.value = false
        advanceTimeBy(SETTLE_MILLIS + 1)
        assertEquals(1, moments.size)
    }
}
