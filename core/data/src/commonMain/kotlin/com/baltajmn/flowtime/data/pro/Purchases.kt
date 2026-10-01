package com.baltajmn.flowtime.data.pro

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.IS_PRO
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.IS_SUPPORTER
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lo que se vende en Play. Los ids son para siempre: uno borrado no se puede volver a usar. */
object Products {
    const val PRO = "pro_lifetime"
    val TIPS = listOf("tip_small", "tip_medium", "tip_large")
}

/** Los derechos de RevenueCat: Pro, y la insignia de quien ha dejado alguna propina. */
object Entitlements {
    const val PRO = "pro"
    const val SUPPORTER = "supporter"
}

/** Un producto, con el precio que pone la tienda para el país del usuario ya formateado. */
data class CatalogItem(val id: String, val price: String)

data class Catalog(val pro: CatalogItem?, val tips: List<CatalogItem>)

enum class PurchaseOutcome { Success, Cancelled, Failed }

/** Desde dónde se compra: en Android, la Activity sobre la que la tienda enseña su diálogo. */
expect open class PurchaseHost

/** Pro y las propinas: la única comprobación de toda la app. */
interface PurchasesRepository {
    val isPro: StateFlow<Boolean>
    val isSupporter: StateFlow<Boolean>

    /** Vuelve a preguntar a la tienda. Si falla, lo guardado se queda como estaba. */
    suspend fun refresh()

    /** Lo que se vende, o null sin conexión o sin Google Play. */
    suspend fun catalog(): Catalog?

    suspend fun purchase(host: PurchaseHost, item: CatalogItem): PurchaseOutcome

    /** Si ha encontrado Pro o alguna propina. */
    suspend fun restore(): Boolean
}

/** La tienda: RevenueCat en la app y una falsa en los tests. Cada llamada lanza si falla. */
interface Store {
    /** Los derechos activos. */
    suspend fun entitlements(): Set<String>

    suspend fun catalog(): Catalog

    /** Los derechos después de comprar, o null si el usuario ha cancelado. */
    suspend fun purchase(host: PurchaseHost, item: CatalogItem): Set<String>?

    suspend fun restore(): Set<String>

    /** Los cambios que llegan solos: una compra pendiente que se completa, o un reembolso. */
    fun onChange(listener: (Set<String>) -> Unit)
}

/**
 * Lo último que dijo la tienda queda guardado, así que funciona sin conexión y desde el primer
 * fotograma. Sin tienda (sin clave de RevenueCat) todo es gratis y no se conecta a nada.
 */
class DefaultPurchasesRepository(
    private val store: Store?,
    private val prefs: DataProvider
) : PurchasesRepository {

    private val pro = MutableStateFlow(prefs.getBoolean(IS_PRO, false))
    private val supporter = MutableStateFlow(prefs.getBoolean(IS_SUPPORTER, false))
    override val isPro: StateFlow<Boolean> = pro.asStateFlow()
    override val isSupporter: StateFlow<Boolean> = supporter.asStateFlow()

    init {
        store?.onChange(::save)
    }

    override suspend fun refresh() {
        val store = store ?: return
        runCatching { store.entitlements() }.onSuccess(::save)
    }

    override suspend fun catalog(): Catalog? = store?.let { runCatching { it.catalog() }.getOrNull() }

    override suspend fun purchase(host: PurchaseHost, item: CatalogItem): PurchaseOutcome {
        val store = store ?: return PurchaseOutcome.Failed
        val entitlements = runCatching { store.purchase(host, item) }
            .getOrElse { return PurchaseOutcome.Failed }
            ?: return PurchaseOutcome.Cancelled
        save(entitlements)
        return PurchaseOutcome.Success
    }

    override suspend fun restore(): Boolean {
        val entitlements = store?.let { runCatching { it.restore() }.getOrNull() } ?: return false
        save(entitlements)
        return entitlements.isNotEmpty()
    }

    // Pro sigue a la tienda, también cuando lo quita un reembolso. La insignia no se quita nunca: con
    // Billing 8, una propina ya consumida no se puede recuperar desde Google.
    private fun save(entitlements: Set<String>) {
        pro.value = Entitlements.PRO in entitlements
        supporter.value = supporter.value || Entitlements.SUPPORTER in entitlements
        prefs.setBoolean(IS_PRO, pro.value)
        prefs.setBoolean(IS_SUPPORTER, supporter.value)
    }
}
