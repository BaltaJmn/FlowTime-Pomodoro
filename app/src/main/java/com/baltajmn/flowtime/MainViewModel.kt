package com.baltajmn.flowtime

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_RATING
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.THEME_COLOR

class MainViewModel(private val dataProvider: DataProvider) : ViewModel() {

    private val appTheme = AppTheme.valueOf(dataProvider.getString(THEME_COLOR) ?: "Blue")
    private var showRating = dataProvider.getBoolean(SHOW_RATING)

    fun getAppTheme(): AppTheme = appTheme
    fun getShowRating(): Boolean = showRating
    fun setShowRating(hasRate: Boolean) {
        showRating = hasRate
        dataProvider.setBoolean(SHOW_RATING, hasRate)
    }
}