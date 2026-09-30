package com.baltajmn.flowtime.navigation.main

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.core.util.Consumer
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.core.navigation.MainGraph.Edit
import com.baltajmn.flowtime.core.navigation.MainGraph.FlowTime
import com.baltajmn.flowtime.core.navigation.MainGraph.History
import com.baltajmn.flowtime.core.navigation.MainGraph.Home
import com.baltajmn.flowtime.core.navigation.MainGraph.Percentage
import com.baltajmn.flowtime.core.navigation.MainGraph.Pomodoro
import com.baltajmn.flowtime.core.navigation.MainGraph.Settings
import com.baltajmn.flowtime.core.navigation.MainGraph.TodoList
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.edit.EditScreen
import com.baltajmn.flowtime.features.screens.history.HistoryScreen
import com.baltajmn.flowtime.features.screens.home.HomeScreen
import com.baltajmn.flowtime.features.screens.settings.SettingsScreen
import com.baltajmn.flowtime.features.screens.timer.TimerScreen
import com.baltajmn.flowtime.features.screens.todoList.TodoListScreen
import com.baltajmn.flowtime.session.SessionNotification
import com.baltajmn.flowtime.ui.FlowTimeAppState

@Composable
fun MainGraph(
    appState: FlowTimeAppState,
    todoListState: LazyListState,
    settingsState: LazyListState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateUp: () -> Unit,
    navigateToHistory: () -> Unit
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
            TimerScreen(TimerMode.FLOW_TIME)
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
            TimerScreen(TimerMode.POMODORO)
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
            TimerScreen(TimerMode.PERCENTAGE)
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
                navigateToIntro = appState::navigateToOnBoard,
                showSound = showSound,
                onSoundChange = onSoundChange
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

    // Después del NavHost, que ya tiene el grafo: desde la notificación se abre el temporizador, y
    // también al terminar la introducción con "Empezar".
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity) {
        val open = Consumer<Intent> { intent ->
            SessionNotification.timerToOpen(intent)?.let(appState::navigateToTimer)
        }
        appState.takeTimerOnStart()?.let(appState::navigateToTimer)
        activity?.intent?.let(open::accept)
        activity?.addOnNewIntentListener(open)
        onDispose { activity?.removeOnNewIntentListener(open) }
    }
}