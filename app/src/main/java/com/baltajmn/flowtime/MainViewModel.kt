package com.baltajmn.flowtime

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.THEME_COLOR

class MainViewModel(dataProvider: DataProvider) : ViewModel() {
    private val appTheme =
        AppTheme.valueOf(dataProvider.getString(THEME_COLOR) ?: "Blue")

    fun getAppTheme(): AppTheme = appTheme
}