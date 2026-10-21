package com.baltajmn.flowtime.data.pro

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.PRO_OFFERED_AT
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.REVIEW_ASKED_AT
import com.baltajmn.flowtime.data.repository.SessionRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.coroutines.flow.first

/**
 * El momento de Pro (D9): se ofrece UNA vez, a quien ya ha hecho [MIN_SESSIONS] sesiones de trabajo,
 * acaba de cerrar un buen momento (15 minutos hoy) y no es Pro ni lo fue. Sin sesión en marcha. Y no a
 * la vez que la petición de valoración: si esa acaba de salir, espera al siguiente momento tranquilo.
 *
 * Lo que se ofrece es la hoja de Pro de siempre, que se cierra con la X: no bloquea ni quita nada, y
 * Coop sigue gratis. El precio no cambia (1,99 el vitalicio, 0,99 al mes).
 */
class ProOfferPolicy(
    private val dataProvider: DataProvider,
    private val sessions: SessionRepository,
    private val isPro: () -> Boolean,
    /** Tuvo un mensual y caducó: ya conoce Pro. */
    private val proEnded: () -> Boolean,
    private val sessionRunning: () -> Boolean,
    private val enabled: Boolean = ProFeatures.enabled,
    private val clock: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    suspend fun shouldOffer(): Boolean {
        if (!enabled || isPro() || proEnded() || sessionRunning()) return false
        if (dataProvider.getLong(PRO_OFFERED_AT) != 0L) return false
        val now = clock()
        val reviewAskedAt = dataProvider.getLong(REVIEW_ASKED_AT)
        if (reviewAskedAt != 0L && now - reviewAskedAt <= AFTER_REVIEW_MILLIS) return false
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(TimeZone.currentSystemDefault()).date
        if (sessions.secondsOn(today).first() < MIN_TODAY_SECONDS) return false
        return sessions.countSessions(FIRST_DAY, LAST_DAY) >= MIN_SESSIONS
    }

    /** Ofrecida: no vuelve a salir sola. */
    fun offered() = dataProvider.setLong(PRO_OFFERED_AT, clock())

    companion object {
        const val MIN_SESSIONS = 5
        val MIN_TODAY_SECONDS = 15.minutes.inWholeSeconds
        val AFTER_REVIEW_MILLIS = 1.hours.inWholeMilliseconds
        private val FIRST_DAY = LocalDate(1970, 1, 1)
        private val LAST_DAY = LocalDate(9999, 12, 31)
    }
}
