package com.baltajmn.flowtime.features.screens.todoList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.common.extensions.toShowInList
import com.baltajmn.flowtime.features.screens.todoList.domain.GetTodoListByDateUseCase
import com.baltajmn.flowtime.features.screens.todoList.domain.InsertTodoListUseCase
import com.baltajmn.flowtime.features.screens.todoList.domain.UpdateTodoListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TodoListViewModel(
    private val getTodoListByDate: GetTodoListByDateUseCase,
    private val updateTodoList: UpdateTodoListUseCase,
    private val insertTodoList: InsertTodoListUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodoListState())
    val uiState: StateFlow<TodoListState> = _uiState

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    currentTodoList = getTodoListByDate(_uiState.value.selectedDate.toString())
                )
            }
        }
    }

    fun plusDay() {
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate.plusDays(1)
            _uiState.update {
                it.copy(
                    selectedDate = selectedDate,
                    selectedDateToShow = selectedDate.toShowInList(),
                    currentTodoList = getTodoListByDate(selectedDate.toString())
                )
            }
        }
    }

    fun minusDay() {
        viewModelScope.launch {
            val selectedDate = _uiState.value.selectedDate.minusDays(1)
            _uiState.update {
                it.copy(
                    selectedDate = selectedDate,
                    selectedDateToShow = selectedDate.toShowInList(),
                    currentTodoList = getTodoListByDate(selectedDate.toString())
                )
            }
        }
    }
}