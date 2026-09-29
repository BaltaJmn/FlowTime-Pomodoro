package com.baltajmn.flowtime.data.review

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.REVIEW_ASKED_AT
import com.baltajmn.flowtime.data.repository.SessionRepository
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.transformLatest

/**
 * A quién pedir la valoración: a quien ya lleva unos días con la app y varias sesiones, y como mucho
 * una vez cada 120 días. Sin diálogo propio: Play decide si enseña el suyo.
 */
class ReviewPolicy(
    private val dataProvider: DataProvider,
    private val sessions: SessionRepository,
    private val sessionRunning: () -> Boolean,
    private val installedAt: () -> Long,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun shouldAsk(): Boolean {
        if (sessionRunning()) return false
        val now = clock()
        if (now - installedAt() <= MIN_INSTALLED_MILLIS) return false
        val askedAt = dataProvider.getLong(REVIEW_ASKED_AT)
        if (askedAt != 0L && now - askedAt <= MIN_BETWEEN_MILLIS) return false
        return sessions.countSessions(FIRST_DAY, LAST_DAY) >= MIN_SESSIONS
    }

    /** Pedida, la enseñe Play o no: hasta dentro de 120 días, nada. */
    fun asked() = dataProvider.setLong(REVIEW_ASKED_AT, clock())

    companion object {
        const val MIN_SESSIONS = 3
        val MIN_INSTALLED_MILLIS = TimeUnit.DAYS.toMillis(2)
        val MIN_BETWEEN_MILLIS = TimeUnit.DAYS.toMillis(120)
        private val FIRST_DAY = LocalDate.of(1970, 1, 1)
        private val LAST_DAY = LocalDate.of(9999, 12, 31)
    }
}

/** Lo que se espera a que llegue la celebración del bloque que acaba de cumplir el objetivo. */
const val SETTLE_MILLIS = 1_500L

/**
 * Los momentos tranquilos para pedirla: al terminar una sesión, o al cerrar la celebración del
 * objetivo sin una en marcha. Nunca con lo que hay al empezar a mirar, es decir, al abrir la app; ni
 * durante el trabajo, el descanso o la celebración.
 */
fun calmMoments(
    sessionRunning: Flow<Boolean>,
    celebrating: Flow<Boolean>,
    settleMillis: Long = SETTLE_MILLIS
): Flow<Unit> = combine(sessionRunning, celebrating) { running, party -> !running && !party }
    .distinctUntilChanged()
    .drop(1)
    .transformLatest { calm ->
        if (calm) {
            delay(settleMillis)
            emit(Unit)
        }
    }
