package com.baltajmn.flowtime.navigation.main

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import com.baltajmn.flowtime.features.screens.focus.sessionRunning
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

private const val MAX_LABEL_SCALE = 1.3f

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
    // Desde cualquier otra pestaña se ve que hay una sesión en marcha.
    val running = sessionRunning()
    val todoListState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            Destination.entries.forEach { destination ->
                item(
                    selected = currentRoute == destination.graph.route,
                    onClick = { appState.navigateTo(destination.graph) },
                    icon = {
                        BadgedBox(
                            badge = { if (destination == Destination.Focus && running) Badge() }
                        ) {
                            Icon(
                                painter = painterResource(destination.icon),
                                contentDescription = null
                            )
                        }
                    },
                    // La etiqueta crece con la letra del sistema hasta 1,3 (el mayor tamaño normal en
                    // las dos) y ahí se queda, como la barra de iOS; si aun así no cabe ("Einstellungen"
                    // en un móvil estrecho), encoge antes que partirse. README, "Barra de pestañas".
                    label = {
                        val density = LocalDensity.current
                        CompositionLocalProvider(
                            LocalDensity provides if (density.fontScale > MAX_LABEL_SCALE) Density(density.density, MAX_LABEL_SCALE) else density
                        ) {
                            Text(
                                text = stringResource(destination.label),
                                // La última pestaña llega al borde de la pantalla: sin este aire, "Einstellungen" lo rozaba.
                                modifier = Modifier.padding(horizontal = 4.dp),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = LocalTextStyle.current.fontSize)
                            )
                        }
                    }
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
