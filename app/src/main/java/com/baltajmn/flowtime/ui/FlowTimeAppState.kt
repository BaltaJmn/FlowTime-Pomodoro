package com.baltajmn.flowtime.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
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
    mainNavController: NavHostController = rememberNavController(),
    context: Context = LocalContext.current
) = remember(preMainNavController, mainNavController, context) {
    FlowTimeAppState(preMainNavController, mainNavController, context)
}

@Stable
class FlowTimeAppState(
    val preMainNavController: NavHostController,
    val mainNavController: NavHostController,
    private val context: Context
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
