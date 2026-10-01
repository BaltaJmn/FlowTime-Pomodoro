package com.baltajmn.flowtime.navigation.main

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.core.navigation.MainGraph
import com.baltajmn.flowtime.ui.FlowTimeAppState
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class Destination(
    val graph: MainGraph,
    val icon: DrawableResource,
    val label: StringResource
) {
    Focus(MainGraph.Focus, Res.drawable.ic_timer, Res.string.nav_focus),
    Tasks(MainGraph.TodoList, Res.drawable.ic_list, Res.string.nav_todo_list),
    Stats(MainGraph.Stats, Res.drawable.ic_stats, Res.string.nav_stats),
    Settings(MainGraph.Settings, Res.drawable.ic_settings, Res.string.nav_settings)
}

/**
 * Barra inferior con los 4 destinos, que en una tablet o en horizontal pasa a barra lateral. La
 * sesión sigue en el motor: se puede ir a cualquier pantalla sin pararla.
 */
@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    openFocus: Flow<Unit>,
    onAddQuickTile: (() -> Unit)?
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
            onSoundChange = onSoundChange,
            openFocus = openFocus,
            onAddQuickTile = onAddQuickTile
        )
    }
}
