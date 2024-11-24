package com.baltajmn.flowtime.core.common.model

data class TodoList(
    val date: String = "",
    val todoList: List<ListItem> = emptyList()
) {
    companion object {
        fun empty() = TodoList()
    }
}

data class ListItem(
    val id: Long = 0,
    val title: String = "",
    val description: String = "",
    val done: Boolean = false
)