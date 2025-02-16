package com.baltajmn.flowtime.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.baltajmn.flowtime.core.design.components.BottomNavBarItem
import com.baltajmn.flowtime.core.navigation.GRAPH
import com.baltajmn.flowtime.core.navigation.MainGraph
import com.baltajmn.flowtime.core.navigation.PreMainGraph
import com.baltajmn.flowtime.core.navigation.extensions.navigateAndPop
import com.baltajmn.flowtime.core.navigation.extensions.navigatePoppingUpToStartDestination

@Composable
fun rememberAppState(
    authNavController: NavHostController = rememberNavController(),
    mainNavController: NavHostController = rememberNavController(),
    context: Context = LocalContext.current
) = remember(authNavController, mainNavController, context) {
    FlowTimeAppState(authNavController, mainNavController, context)
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

    fun navigateUp() {
        mainNavController.navigateUp()
    }

    fun bottomNavigationTo(bottomNavBarItem: BottomNavBarItem) {
        when (bottomNavBarItem) {
            BottomNavBarItem.FlowTime -> navigateToFlowTime()
            BottomNavBarItem.Pomodoro -> navigateToPomodoro()
            BottomNavBarItem.Percentage -> navigateToPercentage()
            BottomNavBarItem.TodoList -> navigateToTodoList()
            BottomNavBarItem.Settings -> navigateToSettings()
        }
    }

    private fun navigateToFlowTime() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.FlowTime.route)
    }

    private fun navigateToPomodoro() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.Pomodoro.route)
    }

    private fun navigateToPercentage() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.Percentage.route)
    }

    private fun navigateToTodoList() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.TodoList.route)
    }

    private fun navigateToSettings() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.Settings.route)
    }

    fun navigateToHistory() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.History.route)
    }
}