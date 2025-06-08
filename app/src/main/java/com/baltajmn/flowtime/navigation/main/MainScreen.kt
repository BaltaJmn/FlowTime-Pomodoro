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
import com.baltajmn.flowtime.core.design.model.ScreenType
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.navigation.MainGraph
import com.baltajmn.flowtime.ui.FlowTimeAppState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit
) {
    var screenRoute by remember { mutableStateOf(BottomNavBarItem.Home) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current

    val currentRoute = appState.currentRoute

    val todoListState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    val settingsStateScrolling = settingsState.isScrollingUp()

    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    val shouldShow = !isTimerRunning || (isPortrait && settingsStateScrolling)

    Scaffold(
        topBar = {
            TopNavBar(shouldShow = { shouldShow && showSound })
        },
        bottomBar = {
            BottomNavBar(
                shouldShow = { shouldShow && isTimerRunning.not() },
                currentRoute = { currentRoute },
                onSelectedItem = { navBarItem, screenType ->
                    screenRoute = navBarItem
                    if (currentRoute != navBarItem.getScreenRoute()) {
                        if (isTimerRunning.not()) {
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
    ) { _ ->
        MainGraph(
            appState = appState,
            todoListState = todoListState,
            settingsState = settingsState,
            navigateToHistory = { appState.navigateToHistory() },
            navigateUp = { appState.navigateUp() },
            onThemeChanged = onThemeChanged,
            showSound = showSound,
            onSoundChange = onSoundChange,
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
                    appState.bottomNavigationTo(
                        bottomNavBarItem = screenRoute,
                        type = ScreenType.FlowTime
                    )
                }
            }
        )
    }
}