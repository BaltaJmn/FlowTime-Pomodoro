package com.baltajmn.flowtime.navigation.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.navigation.MainGraph
import com.baltajmn.flowtime.ui.FlowTimeAppState

private enum class Destination(
    val graph: MainGraph,
    @DrawableRes val icon: Int,
    @StringRes val label: Int
) {
    Focus(MainGraph.Focus, R.drawable.ic_timer, R.string.nav_focus),
    Tasks(MainGraph.TodoList, R.drawable.ic_list, R.string.nav_todo_list),
    Stats(MainGraph.Stats, R.drawable.ic_stats, R.string.nav_stats),
    Settings(MainGraph.Settings, R.drawable.ic_settings, R.string.nav_settings)
}

/**
 * Barra inferior con los 4 destinos, que en una tablet o en horizontal pasa a barra lateral. La
 * sesión sigue en el motor: se puede ir a cualquier pantalla sin pararla.
 */
@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit
) {
    val currentRoute = appState.currentRoute
    val todoListState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.entries.forEach { destination ->
                item(
                    selected = currentRoute == destination.graph.route,
                    onClick = { appState.navigateTo(destination.graph) },
                    icon = {
                        Icon(
                            painter = painterResource(destination.icon),
                            contentDescription = null
                        )
                    },
                    label = { Text(text = stringResource(destination.label)) }
                )
            }
        }
    ) {
        MainGraph(
            appState = appState,
            todoListState = todoListState,
            settingsState = settingsState,
            showSound = showSound,
            onSoundChange = onSoundChange
        )
    }
}
