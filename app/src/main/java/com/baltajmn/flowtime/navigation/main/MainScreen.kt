package com.baltajmn.flowtime.navigation.main

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.baltajmn.flowtime.core.design.components.BottomNavBar
import com.baltajmn.flowtime.core.design.components.BottomNavBarItem
import com.baltajmn.flowtime.core.design.components.TimerAlertDialog
import com.baltajmn.flowtime.core.design.components.isScrollingUp
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.navigation.MainGraph.FlowTime
import com.baltajmn.flowtime.core.navigation.MainGraph.Pomodoro
import com.baltajmn.flowtime.ui.FlowTimeAppState

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen(
    appState: FlowTimeAppState,
    onThemeChanged: (AppTheme) -> Unit
) {
    HideSystemBars()

    var screenRoute by remember { mutableStateOf(BottomNavBarItem.FlowTime) }
    var isTimerRunning by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current

    val currentRoute = appState.currentRoute
    val flowTimeState = rememberLazyListState()
    val pomodoroState = rememberLazyListState()
    val settingsState = rememberLazyListState()

    val settingsStateScrolling = settingsState.isScrollingUp()

    val shouldShow =
        (configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                && settingsStateScrolling
                || currentRoute == FlowTime.route
                || currentRoute == Pomodoro.route

    Scaffold(
        bottomBar = {
            BottomNavBar(
                shouldShow = { shouldShow },
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
            settingsState = settingsState,
            onThemeChanged = onThemeChanged,
        ) { isRunning ->
            isTimerRunning = isRunning
        }
    }

    if (showDialog) {
        TimerAlertDialog(
            isOpen = showDialog,
            onCloseDialog = {
                showDialog = false
                if (it){
                    appState.bottomNavigationTo(screenRoute)
                }
            }
        )
    }
}

@Composable
fun HideSystemBars() {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        val window = context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)

        insetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        onDispose {
            insetsController.apply {
                show(WindowInsetsCompat.Type.statusBars())
                show(WindowInsetsCompat.Type.navigationBars())
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}