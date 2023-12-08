package com.baltajmn.flowtime.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.navigation.main.MainScreen

@Composable
fun FlowTimeNavHost(
    flowTimeAppState: FlowTimeAppState,
    onThemeChanged: (AppTheme) -> Unit
) {

    NavHost(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.background),
        navController = flowTimeAppState.authNavController,
        route = GRAPH.Root,
        startDestination = GRAPH.Main,
    ) {
        composable(GRAPH.Main) {
            MainScreen(
                appState = flowTimeAppState,
                onThemeChanged = onThemeChanged,
            )
        }
    }

}