package com.baltajmn.flowtime.features.screens.todoList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.data.task.Task
import com.baltajmn.flowtime.data.task.TaskRepository
import com.baltajmn.flowtime.data.goal.today as systemToday
import com.baltajmn.flowtime.data.task.TaskResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** Las tareas del día elegido. Salen de un flujo: cualquier cambio se ve solo. */
class TodoListViewModel(
    private val tasks: TaskRepository,
    private val today: () -> LocalDate = { systemToday() }
) : ViewModel() {

    private val selectedDate = MutableStateFlow(today())

    private val _uiState = MutableStateFlow(
        TodoListState(
            selectedDate = selectedDate.value,
            selectedDateToShow = selectedDate.value.toShowInList(),
            today = selectedDate.value
        )
    )
    val uiState: StateFlow<TodoListState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            selectedDate.flatMapLatest { day ->
                val now = today()
                tasks.day(day, now).map { list -> Triple(day, now, list) }
            }.collect { (day, now, list) ->
                _uiState.update {
                    it.copy(
                        selectedDate = day,
                        selectedDateToShow = day.toShowInList(),
                        today = now,
                        tasks = list
                    )
                }
            }
        }
    }

    fun plusDay() = selectedDate.update { it.plus(1, DateTimeUnit.DAY) }

    fun minusDay() = selectedDate.update { it.minus(1, DateTimeUnit.DAY) }

    fun onAddItem(title: String, description: String) =
        change { tasks.add(title, description, selectedDate.value) }

    fun onUpdateItem(task: Task, title: String, description: String) =
        change { tasks.edit(task.id, title, description) }

    fun markAsDone(task: Task) {
        viewModelScope.launch { tasks.setDone(task.id, !task.done, today()) }
    }

    fun moveToToday(task: Task) {
        viewModelScope.launch { tasks.moveTo(task.id, today()) }
    }

    fun onDeleteItem(task: Task) {
        viewModelScope.launch {
            tasks.delete(task.id)?.let { deleted -> _uiState.update { it.copy(deleted = deleted) } }
        }
    }

    fun undoDelete() {
        val task = _uiState.value.deleted ?: return
        _uiState.update { it.copy(deleted = null) }
        viewModelScope.launch { tasks.restore(task) }
    }

    fun onDeleteShown() = _uiState.update { it.copy(deleted = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun change(action: suspend () -> TaskResult) {
        viewModelScope.launch {
            val message = when (action()) {
                is TaskResult.Done -> null
                TaskResult.Invalid -> TaskMessage.EMPTY
                TaskResult.LimitReached -> TaskMessage.LIMIT
            }
            _uiState.update { it.copy(message = message) }
        }
    }
}
