package com.baltajmn.flowtime.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.core.navigation.MainGraph
import com.baltajmn.flowtime.core.navigation.PreMainGraph
import com.baltajmn.flowtime.core.navigation.extensions.navigateAndPop
import com.baltajmn.flowtime.core.navigation.extensions.navigatePoppingUpToStartDestination

@Composable
fun rememberAppState(
    preMainNavController: NavHostController = rememberNavController(),
    mainNavController: NavHostController = rememberNavController()
) = remember(preMainNavController, mainNavController) {
    FlowTimeAppState(preMainNavController, mainNavController)
}

@Stable
class FlowTimeAppState(
    val preMainNavController: NavHostController,
    val mainNavController: NavHostController
) {

    val currentRoute: String
        @Composable get() = mainNavController.currentBackStackEntryAsState().value?.destination?.route
            ?: ""

    fun navigateToMainGraph() {
        preMainNavController.popBackStack()
        preMainNavController.navigateAndPop(GRAPH.Main)
    }

    fun navigateToOnBoard() {
        preMainNavController.navigate(PreMainGraph.Onboard.route)
    }

    /** Abierta desde Ajustes, la introducción vuelve allí. */
    fun closeOnBoard() {
        preMainNavController.popBackStack()
    }

    fun navigateTo(destination: MainGraph) {
        mainNavController.navigatePoppingUpToStartDestination(destination.route)
    }

    fun navigateToFocus() = navigateTo(MainGraph.Focus)
}
