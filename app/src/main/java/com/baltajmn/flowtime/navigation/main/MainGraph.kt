package com.baltajmn.flowtime.navigation.main

import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.util.Consumer
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
import com.baltajmn.flowtime.session.FocusTileService
import com.baltajmn.flowtime.session.SessionNotification
import com.baltajmn.flowtime.ui.FlowTimeAppState

@Composable
fun MainGraph(
    appState: FlowTimeAppState,
    todoListState: LazyListState,
    settingsState: LazyListState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit
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
            val context = LocalContext.current
            SettingsScreen(
                listState = settingsState,
                navigateToIntro = appState::navigateToOnBoard,
                showSound = showSound,
                onSoundChange = onSoundChange,
                onAddQuickTile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    { FocusTileService.requestAdd(context) }
                } else {
                    null
                }
            )
        }
    }

    // Después del NavHost, que ya tiene el grafo: la notificación abre la pantalla de concentración,
    // que ya enseña el modo de la sesión en marcha.
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity) {
        val open = Consumer<Intent> { intent ->
            if (SessionNotification.timerToOpen(intent) != null) appState.navigateToFocus()
        }
        activity?.intent?.let(open::accept)
        activity?.addOnNewIntentListener(open)
        onDispose { activity?.removeOnNewIntentListener(open) }
    }
}
