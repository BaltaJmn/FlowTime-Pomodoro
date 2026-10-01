package com.baltajmn.flowtime.data.pro

import android.content.Context
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener

/**
 * La clave pública de Android del proyecto de RevenueCat. Va dentro del APK, así que no es secreta.
 * A null, la app funciona entera en gratis y sin conectarse a nada.
 */
val REVENUECAT_API_KEY: String? = "goog_MZekCqcObnOsAXjhcszqRrVrHXN"

/** Las compras, con un usuario anónimo de RevenueCat: FlowTime no tiene cuentas. */
class RevenueCatStore(context: Context, apiKey: String) : Store {

    init {
        Purchases.logLevel = LogLevel.WARN
        Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
    }

    private val purchases get() = Purchases.sharedInstance

    override suspend fun entitlements() = purchases.awaitCustomerInfo().active()

    override suspend fun catalog(): Catalog {
        val packages = packages()
        fun item(id: String) = packages[id]?.let { CatalogItem(id, it.product.price.formatted) }
        return Catalog(pro = item(Products.PRO), tips = Products.TIPS.mapNotNull(::item))
    }

    override suspend fun purchase(host: PurchaseHost, item: CatalogItem): Set<String>? {
        val pack = packages()[item.id] ?: error("${item.id} no está en la oferta actual")
        return try {
            purchases.awaitPurchase(PurchaseParams.Builder(host, pack).build()).customerInfo.active()
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) null else throw e
        }
    }

    override suspend fun restore() = purchases.awaitRestore().active()

    override fun onChange(listener: (Set<String>) -> Unit) {
        purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener { listener(it.active()) }
    }

    // La oferta la guarda el propio SDK: pedirla otra vez al comprar no vuelve a ir a la red.
    private suspend fun packages(): Map<String, Package> =
        purchases.awaitOfferings().current?.availablePackages.orEmpty()
            .associateBy { it.product.id.substringBefore(':') }

    private fun CustomerInfo.active(): Set<String> = entitlements.active.keys
}
