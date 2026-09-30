package com.baltajmn.flowtime.core.navigation

object GRAPH {
    val Root = "root_graph"
    val PreMain = "auth_graph"
    val Main = "main_graph"
}

enum class PreMainGraph(val route: String) {
    Splash("splash"),
    Onboard("onboard")
}

/** Los destinos de la barra de navegación (#51), en su orden. */
enum class MainGraph(val route: String) {
    Focus("focus"),
    TodoList("todoList"),
    Stats("stats"),
    Settings("settings")
}