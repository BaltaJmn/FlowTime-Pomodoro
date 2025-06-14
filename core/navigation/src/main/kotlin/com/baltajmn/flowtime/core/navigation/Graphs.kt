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

enum class MainGraph(val route: String) {
    Home("home"),
    FlowTime("flowTime"),
    Pomodoro("pomodoro"),
    Percentage("percentage"),
    Edit("edit/{type}"),
    TodoList("todoList"),
    Settings("settings"),
    History("history"),
}