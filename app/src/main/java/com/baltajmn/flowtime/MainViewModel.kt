package com.baltajmn.flowtime

import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND

class MainViewModel(dataProvider: DataProvider) : ViewModel() {

    private val showSound = dataProvider.getBoolean(SHOW_SOUND)
    private var productDetailsList = emptyList<ProductDetails>()

    fun getShowSound(): Boolean = showSound

    fun getProductDetailsList(): List<ProductDetails> = productDetailsList
    fun setProductDetailsList(list: List<ProductDetails>) {
        productDetailsList = list
    }
}
