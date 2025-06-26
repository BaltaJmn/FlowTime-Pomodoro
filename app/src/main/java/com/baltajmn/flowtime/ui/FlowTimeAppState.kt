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
import com.baltajmn.flowtime.core.design.model.ScreenType
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

    fun navigateUp() {
        mainNavController.navigateUp()
    }

    fun bottomNavigationTo(bottomNavBarItem: BottomNavBarItem, type: ScreenType) {
        when (bottomNavBarItem) {
            BottomNavBarItem.Edit -> navigateToEdit(type)
            BottomNavBarItem.Back, BottomNavBarItem.Home -> navigateToHome()
            BottomNavBarItem.TodoList -> navigateToTodoList()
            BottomNavBarItem.Settings -> navigateToSettings()
        }
    }

    private fun navigateToHome() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.Home.route)
    }

    fun navigateToFlowTime() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.FlowTime.route)
    }

    fun navigateToPomodoro() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.Pomodoro.route)
    }

    fun navigateToPercentage() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.Percentage.route)
    }

    private fun navigateToEdit(type: ScreenType) {
        val route = MainGraph.Edit.route.replace("{type}", type.name)
        mainNavController.navigate(route)
    }

    fun navigateToTodoList() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.TodoList.route)
    }

    private fun navigateToSettings() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.Settings.route)
    }

    fun navigateToHistory() {
        mainNavController.navigatePoppingUpToStartDestination(MainGraph.History.route)
    }
}