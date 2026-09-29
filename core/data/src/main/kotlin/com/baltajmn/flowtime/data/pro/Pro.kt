package com.baltajmn.flowtime.data.pro

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

/** Si el usuario tiene Pro: la única comprobación de toda la app. */
interface PurchasesRepository {
    val isPro: StateFlow<Boolean>
}

/** Hasta que llegue RevenueCat (#55), nadie tiene Pro. */
class NoPurchases : PurchasesRepository {
    override val isPro: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
}

/** Los límites de la versión gratis, solo si Pro ya existe en esta versión y no se ha comprado. */
class ProGate(
    private val purchases: PurchasesRepository,
    private val enabled: Boolean = ProFeatures.enabled
) {
    /** Si se puede tener uno más cuando ya hay [current] y el límite gratis es [limit]. */
    fun allowsOneMore(current: Int, limit: Int) = !enabled || purchases.isPro.value || current < limit
}
