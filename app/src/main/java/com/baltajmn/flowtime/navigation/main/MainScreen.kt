package com.baltajmn.flowtime.navigation.main

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.baltajmn.flowtime.core.design.components.BottomNavBar
import com.baltajmn.flowtime.core.design.components.isScrollingUp
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.ui.FlowTimeAppState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    onThemeChanged: (AppTheme) -> Unit
) {
    val currentRoute = appState.currentRoute
    val flowTimeState = rememberLazyListState()
    val pomodoroState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    val flowTimeStateScrolling = flowTimeState.isScrollingUp()
    val pomodoroStateScrolling = pomodoroState.isScrollingUp()
    val settingsStateScrolling = settingsState.isScrollingUp()

    val shouldShow = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT


    Scaffold(
        bottomBar = {
            BottomNavBar(
                shouldShow = { shouldShow },
                currentRoute = { currentRoute },
                onSelectedItem = {
                    if (currentRoute != it.getScreenRoute()) {
                        appState.bottomNavigationTo(it)
                    }
                }
            )
        }
    ) { _ ->
        MainGraph(
            appState = appState,
            flowTimeState = flowTimeState,
            pomodoroState = pomodoroState,
            settingsState = settingsState,
            onThemeChanged = onThemeChanged,
        )
    }
}