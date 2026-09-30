package com.baltajmn.flowtime.data.pro

import kotlinx.coroutines.flow.StateFlow

object ProFeatures {
    /**
     * Apagado hasta la versión que publique Pro (#55–#57): mientras, ningún límite se aplica. Lo de
     * Pro nunca se publica gratis para bloquearlo después.
     */
    val enabled: Boolean = false
}

/** Lo que se puede tener a la vez sin Pro. Nunca se borra ni se bloquea nada que ya exista. */
object Limits {
    const val FREE_TAGS = 4
    const val FREE_MIXES = 3
    const val FREE_PENDING_TASKS = 15
}

/** Los límites de la versión gratis, solo si Pro ya existe en esta versión y no se ha comprado. */
class ProGate(
    private val isPro: StateFlow<Boolean>,
    private val enabled: Boolean = ProFeatures.enabled
) {
    /** El límite gratis [free], si se aplica: con Pro a la venta y sin comprar. */
    fun limit(free: Int): Int? = free.takeIf { enabled && !isPro.value }

    /** Si se puede tener uno más cuando ya hay [current] y el límite gratis es [limit]. */
    fun allowsOneMore(current: Int, limit: Int) = limit(limit)?.let { current < it } ?: true
}
