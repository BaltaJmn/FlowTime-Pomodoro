package com.baltajmn.flowtime.navigation.main

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.baltajmn.flowtime.core.design.components.BottomNavBar
import com.baltajmn.flowtime.core.design.components.TopNavBar
import com.baltajmn.flowtime.core.navigation.MainGraph
import com.baltajmn.flowtime.ui.FlowTimeAppState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit
) {
    val currentRoute = appState.currentRoute

    val todoListState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopNavBar(shouldShow = { showSound })
        },
        bottomBar = {
            // La sesión sigue en el motor: se puede ir a cualquier pantalla sin pararla.
            BottomNavBar(
                shouldShow = { true },
                currentRoute = { currentRoute },
                onSelectedItem = { navBarItem, screenType ->
                    if (currentRoute != navBarItem.getScreenRoute()) {
                        if (currentRoute == MainGraph.Edit.route) {
                            appState.navigateUp()
                        } else {
                            appState.bottomNavigationTo(navBarItem, screenType)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // El contenido pasa por debajo de las barras a propósito, pero no por debajo de la cámara
        // ni de la barra de navegación cuando quedan a un lado, en horizontal.
        Box(
            modifier = Modifier.windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
            )
        ) {
            MainGraph(
                appState = appState,
                todoListState = todoListState,
                settingsState = settingsState,
                navigateToHistory = appState::navigateToHistory,
                navigateUp = appState::navigateUp,
                showSound = showSound,
                onSoundChange = onSoundChange
            )
        }
    }
}
