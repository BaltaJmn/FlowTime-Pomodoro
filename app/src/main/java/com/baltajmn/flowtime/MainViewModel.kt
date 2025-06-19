package com.baltajmn.flowtime

import androidx.lifecycle.ViewModel
import com.android.billingclient.api.ProductDetails
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.REMEMBER_SHOW_RATING
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ON_BOARD
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_RATING
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.THEME_COLOR

class MainViewModel(private val dataProvider: DataProvider) : ViewModel() {

    private val appTheme = enumValues<AppTheme>()
        .firstOrNull { it.name == dataProvider.getString(THEME_COLOR) }
        ?: AppTheme.Blue
    private val showSound = dataProvider.getBoolean(SHOW_SOUND)
    private var showOnBoard = dataProvider.getCheckValue(SHOW_ON_BOARD)
    private var showRating = dataProvider.getBoolean(SHOW_RATING)
    private var rememberShowRating = dataProvider.getBoolean(REMEMBER_SHOW_RATING)
    private var productDetailsList = emptyList<ProductDetails>()

    fun getAppTheme(): AppTheme = appTheme

    fun getShowOnBoard(): Boolean = showOnBoard

    fun getShowSound(): Boolean = showSound

    fun getShowRating(): Boolean = showRating
    fun setShowRating(hasRate: Boolean) {
        showRating = hasRate
        dataProvider.setBoolean(SHOW_RATING, hasRate)
    }

    fun getRememberShowRating(): Boolean = rememberShowRating
    fun setRememberShowRating(remember: Boolean) {
        rememberShowRating = remember
        dataProvider.setBoolean(REMEMBER_SHOW_RATING, rememberShowRating)
    }

    fun getProductDetailsList(): List<ProductDetails> = productDetailsList
    fun setProductDetailsList(list: List<ProductDetails>) {
        productDetailsList = list
    }
}