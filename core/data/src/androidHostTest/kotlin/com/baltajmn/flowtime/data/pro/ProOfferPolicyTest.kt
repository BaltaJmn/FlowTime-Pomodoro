package com.baltajmn.flowtime.data.pro

import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.fakes.FakeSessionRepository
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.REVIEW_ASKED_AT
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProOfferPolicyTest {

    private val hour = 60 * 60 * 1000L
    private var now = 1_000_000_000_000L
    private var pro = false
    private var ended = false
    private var running = false
    private var enabled = true
    private val data = FakeDataProvider()
    private val sessions = FakeSessionRepository().apply {
        sessionCount = 5
        secondsEveryDay = 20 * 60L
    }

    private fun policy() = ProOfferPolicy(data, sessions, { pro }, { ended }, { running }, enabled, { now })

    @Test
    fun `con 4 sesiones no y con 5 si`() = runTest {
        sessions.sessionCount = 4
        assertFalse(policy().shouldOffer())

        sessions.sessionCount = ProOfferPolicy.MIN_SESSIONS
        assertTrue(policy().shouldOffer())
    }

    @Test
    fun `solo una vez`() = runTest {
        val policy = policy()
        assertTrue(policy.shouldOffer())
        policy.offered()
        assertFalse(policy.shouldOffer())
        now += 400 * 24 * hour
        assertFalse(policy.shouldOffer())
    }

    @Test
    fun `nunca a quien ya es Pro, lo fue, ni con Pro apagado o una sesion en marcha`() = runTest {
        pro = true
        assertFalse(policy().shouldOffer())
        pro = false
        ended = true
        assertFalse(policy().shouldOffer())
        ended = false
        enabled = false
        assertFalse(policy().shouldOffer())
        enabled = true
        running = true
        assertFalse(policy().shouldOffer())
        running = false
        assertTrue(policy().shouldOffer())
    }

    @Test
    fun `solo tras un buen momento, con 15 minutos hoy`() = runTest {
        sessions.secondsEveryDay = 14 * 60L
        assertFalse(policy().shouldOffer())
        sessions.secondsEveryDay = 15 * 60L
        assertTrue(policy().shouldOffer())
    }

    @Test
    fun `espera si la valoracion acaba de salir`() = runTest {
        data.setLong(REVIEW_ASKED_AT, now - hour / 2)
        assertFalse(policy().shouldOffer())
        now += hour
        assertTrue(policy().shouldOffer())
    }
}
