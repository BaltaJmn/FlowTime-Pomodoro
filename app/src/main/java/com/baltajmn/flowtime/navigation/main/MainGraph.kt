package com.baltajmn.flowtime.navigation.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.baltajmn.flowtime.ui.FlowTimeState
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.core.navigation.MainGraph.FlowTime
import com.baltajmn.flowtime.core.navigation.MainGraph.Pomodoro
import com.baltajmn.flowtime.core.navigation.MainGraph.Settings

@Composable
fun MainGraph(
    appState: FlowTimeState,
    flowTimeState: LazyListState,
    pomodoroState: LazyListState,
    settingsState: LazyListState,
) {

    NavHost(
        navController = appState.mainNavController,
        route = GRAPH.Main,
        startDestination = FlowTime.route
    ) {
        composable(
            route = FlowTime.route
        ) {
            /*FlowTimeScreen(
                listState = flowTimeState
            )*/
        }

        composable(
            route = Pomodoro.route
        ) {
            /*PomodoroScreen(
                listState = pomodoroState
            )*/
        }

        composable(
            route = Settings.route
        ) {
            /*SettingsScreen(
                listState = settingsState
            )*/
        }
    }
}