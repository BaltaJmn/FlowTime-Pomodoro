package com.baltajmn.flowtime.ui

import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme

@Composable
fun FlowTimeApp(
    flowTimeAppState: FlowTimeAppState = rememberAppState(),
    appTheme: AppTheme,
    onThemeChanged: (AppTheme) -> Unit,
) {
    KeepScreenOn()
    FlowTimeTheme(appTheme = appTheme) {
        FlowTimeNavHost(
            flowTimeAppState = flowTimeAppState,
            onThemeChanged = onThemeChanged,
        )
    }
}

@Composable
fun KeepScreenOn() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    val powerManager =
        context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:WakeLockTag")

    DisposableEffect(lifecycleOwner) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> wakeLock.acquire()
                Lifecycle.Event.ON_STOP -> wakeLock.release()
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }
    }
}