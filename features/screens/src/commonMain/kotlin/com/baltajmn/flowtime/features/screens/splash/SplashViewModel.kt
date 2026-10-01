package com.baltajmn.flowtime.features.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ON_BOARD
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val dataProvider: DataProvider
) : ViewModel() {

    private val _event = Channel<Event>()
    val event = _event.receiveAsFlow()

    fun checkHasToShowOnBoard() {
        viewModelScope.launch {
            if (dataProvider.getCheckValue(key = SHOW_ON_BOARD)) {
                _event.send(Event.NavigateToOnBoard)
            } else {
                _event.send(Event.NavigateToMainGraph)
            }
        }
    }

    sealed interface Event {
        data object NavigateToOnBoard : Event
        data object NavigateToMainGraph : Event
    }
}