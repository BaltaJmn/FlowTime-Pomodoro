package com.baltajmn.flowtime.navigation.main

import android.content.res.Configuration
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import com.baltajmn.flowtime.core.design.components.BottomNavBar
import com.baltajmn.flowtime.core.design.components.BottomNavBarItem
import com.baltajmn.flowtime.core.design.components.TimerAlertDialog
import com.baltajmn.flowtime.core.design.components.TopNavBar
import com.baltajmn.flowtime.core.design.components.isScrollingUp
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.navigation.MainGraph
import com.baltajmn.flowtime.ui.FlowTimeAppState

@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit
) {
    var screenRoute by rememberSaveable { mutableStateOf(BottomNavBarItem.Home) }
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val currentRoute = appState.currentRoute

    val todoListState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    val settingsStateScrolling = settingsState.isScrollingUp()
    val isPortrait by derivedStateOf { configuration.orientation == Configuration.ORIENTATION_PORTRAIT }
    val shouldShow by derivedStateOf { !isTimerRunning || (isPortrait && settingsStateScrolling) }

    Scaffold(
        topBar = {
            TopNavBar(shouldShow = { shouldShow && showSound })
        },
        bottomBar = {
            BottomNavBar(
                shouldShow = { shouldShow && !isTimerRunning },
                currentRoute = { currentRoute },
                onSelectedItem = { navBarItem, screenType ->
                    screenRoute = navBarItem
                    val targetRoute = navBarItem.getScreenRoute()
                    if (currentRoute != targetRoute) {
                        if (!isTimerRunning) {
                            if (currentRoute == MainGraph.Edit.route) {
                                appState.navigateUp()
                            } else {
                                appState.bottomNavigationTo(navBarItem, screenType)
                            }
                        } else {
                            showDialog = true
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        MainGraph(
            appState = appState,
            todoListState = todoListState,
            settingsState = settingsState,
            navigateToHistory = appState::navigateToHistory,
            navigateUp = appState::navigateUp,
            onThemeChanged = onThemeChanged,
            showSound = showSound,
            onSoundChange = onSoundChange,
            onSupportDeveloperClick = onSupportDeveloperClick,
            onTimerRunning = { isRunning -> isTimerRunning = isRunning }
        )
    }

    if (showDialog) {
        TimerAlertDialog(
            isOpen = true,
            onCloseDialog = { shouldNavigate ->
                showDialog = false
                if (shouldNavigate) {
                    appState.bottomNavigationTo(
                        bottomNavBarItem = screenRoute,
                        type = ScreenType.FlowTime
                    )
                }
            }
        )
    }
}