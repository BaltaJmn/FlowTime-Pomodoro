package com.baltajmn.flowtime.core.navigation

object GRAPH {
    val Root = "root_graph"
    val Auth = "auth_graph"
    val Main = "main_graph"
}

enum class AuthGraph(val route: String) {
    Splash("splash"),
    Login("login")
}

enum class MainGraph(val route: String) {
    FlowTime("flowTime"),
    Pomodoro("pomodoro"),
    Settings("settings"),
    History("history"),
}