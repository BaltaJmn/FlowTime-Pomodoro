package com.baltajmn.flowtime.features.screens.fakes

import android.app.Activity
import com.baltajmn.flowtime.data.pro.Catalog
import com.baltajmn.flowtime.data.pro.CatalogItem
import com.baltajmn.flowtime.data.pro.Products
import com.baltajmn.flowtime.data.pro.PurchaseOutcome
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Una tienda que siempre responde, con los precios de España. */
class FakePurchases(pro: Boolean = false, supporter: Boolean = false) : PurchasesRepository {
    override val isPro = MutableStateFlow(pro)
    override val isSupporter = MutableStateFlow(supporter)

    override suspend fun refresh() = Unit

    override suspend fun catalog() = CATALOG

    override suspend fun purchase(activity: Activity, item: CatalogItem) = PurchaseOutcome.Success

    override suspend fun restore() = false

    companion object {
        val CATALOG = Catalog(
            pro = CatalogItem(Products.PRO, "2,99 €"),
            tips = Products.TIPS.zip(listOf("1,99 €", "4,99 €", "9,99 €"), ::CatalogItem)
        )
    }
}
