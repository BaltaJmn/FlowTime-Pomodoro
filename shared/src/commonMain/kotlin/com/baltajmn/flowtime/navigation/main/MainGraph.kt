package com.baltajmn.flowtime.navigation.main

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.core.navigation.MainGraph.Focus
import com.baltajmn.flowtime.core.navigation.MainGraph.Settings
import com.baltajmn.flowtime.core.navigation.MainGraph.Stats
import com.baltajmn.flowtime.core.navigation.MainGraph.TodoList
import com.baltajmn.flowtime.features.screens.focus.FocusScreen
import com.baltajmn.flowtime.features.screens.settings.SettingsScreen
import com.baltajmn.flowtime.features.screens.stats.StatsScreen
import com.baltajmn.flowtime.features.screens.todoList.TodoListScreen
import com.baltajmn.flowtime.ui.FlowTimeAppState
import kotlinx.coroutines.flow.Flow

@Composable
fun MainGraph(
    appState: FlowTimeAppState,
    todoListState: LazyListState,
    settingsState: LazyListState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    /** Cada vez que la notificación de la sesión pide abrir la pantalla de concentración. */
    openFocus: Flow<Unit>,
    /** Pide al sistema poner el botón en los ajustes rápidos; null donde no se puede. */
    onAddQuickTile: (() -> Unit)?
) {
    NavHost(
        navController = appState.mainNavController,
        route = GRAPH.Main,
        startDestination = Focus.route
    ) {
        composable(route = Focus.route) {
            FocusScreen(showSound = showSound)
        }

        composable(route = TodoList.route) {
            TodoListScreen(listState = todoListState)
        }

        composable(route = Stats.route) {
            StatsScreen()
        }

        composable(route = Settings.route) {
            SettingsScreen(
                listState = settingsState,
                navigateToIntro = appState::navigateToOnBoard,
                showSound = showSound,
                onSoundChange = onSoundChange,
                onAddQuickTile = onAddQuickTile
            )
        }
    }

    // Después del NavHost, que ya tiene el grafo: la notificación abre la pantalla de concentración,
    // que ya enseña el modo de la sesión en marcha.
    LaunchedEffect(openFocus) { openFocus.collect { appState.navigateToFocus() } }
}
