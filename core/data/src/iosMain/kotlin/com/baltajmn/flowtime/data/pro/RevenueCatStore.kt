package com.baltajmn.flowtime.data.pro

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesConfiguration
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.ktx.awaitRestore
import com.revenuecat.purchases.kmp.models.CustomerInfo
import com.revenuecat.purchases.kmp.models.Package
import com.revenuecat.purchases.kmp.models.PurchasesError
import com.revenuecat.purchases.kmp.models.PurchasesTransactionException
import com.revenuecat.purchases.kmp.models.StoreProduct
import com.revenuecat.purchases.kmp.models.StoreTransaction

/**
 * La clave pública de iOS del proyecto de RevenueCat (`appl_`). Null hasta que existan la cuenta de
 * Apple y la app de iOS en RevenueCat (`~/keys/LEEME.md`): sin ella el iPhone no tiene tienda, y no se
 * publica.
 */
val REVENUECAT_IOS_API_KEY: String? = null

/** Las compras en el iPhone: el mismo proyecto y los mismos derechos que en Android, con purchases-kmp. */
class RevenueCatStore(apiKey: String) : Store {

    init {
        Purchases.logLevel = LogLevel.WARN
        Purchases.configure(PurchasesConfiguration.Builder(apiKey).build())
    }

    private val purchases get() = Purchases.sharedInstance

    override suspend fun entitlements() = purchases.awaitCustomerInfo().active()

    override suspend fun catalog(): Catalog {
        val packages = packages()
        fun item(id: String) = packages[id]?.let { CatalogItem(id, it.storeProduct.price.formatted) }
        return Catalog(pro = item(Products.PRO), tips = Products.TIPS.mapNotNull(::item))
    }

    // La tienda del iPhone pone su propia hoja encima de lo que haya: no necesita la pantalla.
    override suspend fun purchase(host: PurchaseHost, item: CatalogItem): Set<String>? {
        val pack = packages()[item.id] ?: error("${item.id} no está en la oferta actual")
        return try {
            purchases.awaitPurchase(packageToPurchase = pack).customerInfo.active()
        } catch (e: PurchasesTransactionException) {
            if (e.userCancelled) null else throw e
        }
    }

    override suspend fun restore() = purchases.awaitRestore().active()

    override fun onChange(listener: (Set<String>) -> Unit) {
        purchases.delegate = object : PurchasesDelegate {
            override fun onCustomerInfoUpdated(customerInfo: CustomerInfo) = listener(customerInfo.active())

            // Las compras que empiezan en la App Store: no hay ninguna promocionada.
            override fun onPurchasePromoProduct(
                product: StoreProduct,
                startPurchase: (
                    onError: (error: PurchasesError, userCancelled: Boolean) -> Unit,
                    onSuccess: (storeTransaction: StoreTransaction, customerInfo: CustomerInfo) -> Unit
                ) -> Unit
            ) = Unit
        }
    }

    private suspend fun packages(): Map<String, Package> =
        purchases.awaitOfferings().current?.availablePackages.orEmpty().associateBy { it.storeProduct.id }

    private fun CustomerInfo.active(): Set<String> = entitlements.active.keys
}
