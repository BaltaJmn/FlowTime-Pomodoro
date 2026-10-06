package com.baltajmn.flowtime.features.screens.fakes

import android.app.Activity
import com.baltajmn.flowtime.data.pro.Catalog
import com.baltajmn.flowtime.data.pro.CatalogItem
import com.baltajmn.flowtime.data.pro.Products
import com.baltajmn.flowtime.data.pro.PurchaseOutcome
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Una tienda con los precios de España; sin [online], no responde. Comprar Pro lo da al momento. */
class FakePurchases(
    pro: Boolean = false,
    supporter: Boolean = false,
    var online: Boolean = true,
    var outcome: PurchaseOutcome = PurchaseOutcome.Success,
    /** Lo que encuentra "Restaurar compras". */
    var restores: Boolean = false
) : PurchasesRepository {
    override val isPro = MutableStateFlow(pro)
    override val isSupporter = MutableStateFlow(supporter)

    override suspend fun refresh() = Unit

    override suspend fun catalog() = CATALOG.takeIf { online }

    override suspend fun purchase(activity: Activity, item: CatalogItem): PurchaseOutcome {
        if (outcome == PurchaseOutcome.Success && item.id == Products.PRO) isPro.value = true
        return outcome
    }

    override suspend fun restore(): Boolean {
        if (restores) isPro.value = true
        return restores
    }

    companion object {
        val CATALOG = Catalog(
            pro = CatalogItem(Products.PRO, "$1.99"),
            tips = Products.TIPS.zip(listOf("$1.99", "$4.99", "$9.99"), ::CatalogItem)
        )
    }
}
