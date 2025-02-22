package com.baltajmn.flowtime.navigation.main

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import com.baltajmn.flowtime.core.design.components.BottomNavBar
import com.baltajmn.flowtime.core.design.components.BottomNavBarItem
import com.baltajmn.flowtime.core.design.components.TimerAlertDialog
import com.baltajmn.flowtime.core.design.components.TopNavBar
import com.baltajmn.flowtime.core.design.components.isScrollingUp
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.navigation.MainGraph.FlowTime
import com.baltajmn.flowtime.core.navigation.MainGraph.Percentage
import com.baltajmn.flowtime.core.navigation.MainGraph.Pomodoro
import com.baltajmn.flowtime.core.navigation.MainGraph.Settings
import com.baltajmn.flowtime.core.navigation.MainGraph.TodoList
import com.baltajmn.flowtime.ui.FlowTimeAppState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit
) {
    var screenRoute by remember { mutableStateOf(BottomNavBarItem.FlowTime) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current

    val currentRoute = appState.currentRoute
    val flowTimeState = rememberLazyListState()
    val pomodoroState = rememberLazyListState()
    val percentageState = rememberLazyListState()
    val todoListState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    val settingsStateScrolling = settingsState.isScrollingUp()

    val shouldShow =
        (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) && settingsStateScrolling || currentRoute == FlowTime.route || currentRoute == Pomodoro.route || currentRoute == Percentage.route || currentRoute == TodoList.route || currentRoute == Settings.route

    Scaffold(
        topBar = {
            TopNavBar(shouldShow = { shouldShow })
        },
        bottomBar = {
            BottomNavBar(
                shouldShow = { shouldShow && isTimerRunning.not() },
                currentRoute = { currentRoute },
                onSelectedItem = {
                    screenRoute = it
                    if (currentRoute != it.getScreenRoute()) {
                        if (isTimerRunning.not()) {
                            appState.bottomNavigationTo(it)
                        } else {
                            showDialog = true
                        }
                    }
                }
            )
        }
    ) { _ ->
        MainGraph(
            appState = appState,
            flowTimeState = flowTimeState,
            pomodoroState = pomodoroState,
            percentageState = percentageState,
            todoListState = todoListState,
            settingsState = settingsState,
            navigateToHistory = { appState.navigateToHistory() },
            navigateUp = { appState.navigateUp() },
            onThemeChanged = onThemeChanged,
            onSupportDeveloperClick = onSupportDeveloperClick
        ) { isRunning ->
            isTimerRunning = isRunning
        }
    }

    if (showDialog) {
        TimerAlertDialog(
            isOpen = showDialog,
            onCloseDialog = {
                showDialog = false
                if (it) {
                    appState.bottomNavigationTo(screenRoute)
                }
            }
        )
    }
}