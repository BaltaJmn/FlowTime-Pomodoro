package com.baltajmn.flowtime

import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND

class MainViewModel(dataProvider: DataProvider) : ViewModel() {

    private val showSound = dataProvider.getBoolean(SHOW_SOUND)

    fun getShowSound(): Boolean = showSound
}
