package com.baltajmn.flowtime.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.core.navigation.PreMainGraph
import com.baltajmn.flowtime.features.screens.onboard.OnBoardScreen
import com.baltajmn.flowtime.features.screens.splash.SplashScreen
import com.baltajmn.flowtime.navigation.main.MainScreen

@Composable
fun FlowTimeNavHost(
    flowTimeAppState: FlowTimeAppState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit
) {
    NavHost(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.background),
        navController = flowTimeAppState.preMainNavController,
        route = GRAPH.Root,
        startDestination = GRAPH.PreMain
    ) {
        preMainNavGraph(appState = flowTimeAppState)

        composable(GRAPH.Main) {
            MainScreen(
                appState = flowTimeAppState,
                showSound = showSound,
                onSoundChange = onSoundChange,
                onThemeChanged = onThemeChanged,
                onSupportDeveloperClick = onSupportDeveloperClick
            )
        }
    }
}

fun NavGraphBuilder.preMainNavGraph(
    appState: FlowTimeAppState
) {
    navigation(
        route = GRAPH.PreMain,
        startDestination = PreMainGraph.Splash.route
    ) {
        composable(
            route = PreMainGraph.Splash.route
        ) {
            SplashScreen(
                navigateToMainGraph = { appState.navigateToMainGraph() },
                navigateToOnBoard = { appState.navigateToOnBoard() }
            )
        }

        composable(
            route = PreMainGraph.Onboard.route
        ) {
            OnBoardScreen(
                navigateToMainGraph = { appState.navigateToMainGraph() }
            )
        }
    }
}
