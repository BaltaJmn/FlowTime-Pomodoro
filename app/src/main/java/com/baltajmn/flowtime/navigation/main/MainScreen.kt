package com.baltajmn.flowtime.navigation.main

import android.annotation.SuppressLint
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.baltajmn.flowtime.ui.FlowTimeState
import com.baltajmn.flowtime.core.design.components.BottomNavBar
import com.baltajmn.flowtime.core.design.components.isScrollingUp

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    appState: FlowTimeState
) {
    val currentRoute = appState.currentRoute
    val flowTimeState = rememberLazyListState()
    val pomodoroState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    val flowTimeStateScrolling = flowTimeState.isScrollingUp()
    val pomodoroStateScrolling = pomodoroState.isScrollingUp()
    val settingsStateScrolling = settingsState.isScrollingUp()

    val shouldShow = (flowTimeStateScrolling || pomodoroStateScrolling || settingsStateScrolling)


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
        )
    }
}