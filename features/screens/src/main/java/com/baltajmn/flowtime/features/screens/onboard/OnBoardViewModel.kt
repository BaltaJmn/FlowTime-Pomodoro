package com.baltajmn.flowtime.features.screens.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.NavigateToMainGraph
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class OnBoardViewModel(
    private val dataProvider: DataProvider
) : ViewModel() {

    private val _event = Channel<Event>()
    val event = _event.receiveAsFlow()

    fun finishOnBoard() {
        viewModelScope.launch {
            dataProvider.setCheckValue(key = SharedPreferencesItem.SHOW_ON_BOARD, value = false)
            _event.send(NavigateToMainGraph)
        }
    }

    sealed interface Event {
        data object NavigateToMainGraph : Event
    }
}