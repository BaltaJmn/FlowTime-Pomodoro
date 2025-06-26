package com.baltajmn.flowtime.navigation.main

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.core.navigation.MainGraph.Edit
import com.baltajmn.flowtime.core.navigation.MainGraph.FlowTime
import com.baltajmn.flowtime.core.navigation.MainGraph.History
import com.baltajmn.flowtime.core.navigation.MainGraph.Home
import com.baltajmn.flowtime.core.navigation.MainGraph.Percentage
import com.baltajmn.flowtime.core.navigation.MainGraph.Pomodoro
import com.baltajmn.flowtime.core.navigation.MainGraph.Settings
import com.baltajmn.flowtime.core.navigation.MainGraph.TodoList
import com.baltajmn.flowtime.features.screens.edit.EditScreen
import com.baltajmn.flowtime.features.screens.flowtime.FlowTimeScreen
import com.baltajmn.flowtime.features.screens.history.HistoryScreen
import com.baltajmn.flowtime.features.screens.home.HomeScreen
import com.baltajmn.flowtime.features.screens.percentage.PercentageScreen
import com.baltajmn.flowtime.features.screens.pomodoro.PomodoroScreen
import com.baltajmn.flowtime.features.screens.settings.SettingsScreen
import com.baltajmn.flowtime.features.screens.todoList.TodoListScreen
import com.baltajmn.flowtime.ui.FlowTimeAppState

@Composable
fun MainGraph(
    appState: FlowTimeAppState,
    todoListState: LazyListState,
    settingsState: LazyListState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateUp: () -> Unit,
    navigateToHistory: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit,
    onTimerRunning: (Boolean) -> Unit
) {
    NavHost(
        navController = appState.mainNavController,
        route = GRAPH.Main,
        startDestination = Home.route
    ) {
        composable(route = Home.route) {
            HomeScreen { screenType ->
                when (screenType) {
                    ScreenType.Pomodoro -> appState.navigateToPomodoro()
                    ScreenType.FlowTime -> appState.navigateToFlowTime()
                    ScreenType.Percentage -> appState.navigateToPercentage()
                }
            }
        }

        composable(
            route = FlowTime.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Up,
                    animationSpec = tween(500)
                )
            }
        ) {
            FlowTimeScreen(onTimerRunning = onTimerRunning)
        }

        composable(
            route = Pomodoro.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Up,
                    animationSpec = tween(500)
                )
            }
        ) {
            PomodoroScreen(onTimerRunning = onTimerRunning)
        }

        composable(
            route = Percentage.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Up,
                    animationSpec = tween(500)
                )
            }
        ) {
            PercentageScreen(
                onTimerRunning = onTimerRunning
            )
        }

        composable(
            route = Edit.route,
            arguments = listOf(navArgument("type") { type = NavType.StringType }),
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Up,
                    animationSpec = tween(500)
                )
            }
        ) {
            EditScreen()
        }

        composable(
            route = TodoList.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Up,
                    animationSpec = tween(500)
                )
            }
        ) {
            TodoListScreen(
                listState = todoListState
            )
        }

        composable(
            route = Settings.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Up,
                    animationSpec = tween(500)
                )
            }
        ) {
            SettingsScreen(
                listState = settingsState,
                navigateToHistory = navigateToHistory,
                showSound = showSound,
                onSoundChange = onSoundChange,
                onThemeChanged = onThemeChanged,
                onSupportDeveloperClick = onSupportDeveloperClick
            )
        }

        composable(
            route = History.route,
            enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Companion.Up,
                    animationSpec = tween(500)
                )
            }
        ) {
            HistoryScreen(
                navigateUp = navigateUp
            )
        }
    }
}