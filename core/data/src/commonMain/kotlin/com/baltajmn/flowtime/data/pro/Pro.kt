package com.baltajmn.flowtime.data.pro

import kotlinx.coroutines.flow.StateFlow

object ProFeatures {
    /**
     * Encendido desde la 2.1.0, la versión que pone Pro a la venta (#56). Apagado, ningún límite se
     * aplica y no se ofrece Pro. Lo de Pro nunca se publica gratis para bloquearlo después.
     */
    val enabled: Boolean = true
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
    /** Lo de Pro está cerrado: con Pro a la venta y sin comprar. */
    val locked: Boolean get() = enabled && !isPro.value

    /** El límite gratis [free], si se aplica. */
    fun limit(free: Int): Int? = free.takeIf { locked }

    /** Si se puede tener uno más cuando ya hay [current] y el límite gratis es [limit]. */
    fun allowsOneMore(current: Int, limit: Int) = limit(limit)?.let { current < it } ?: true
}
