package com.baltajmn.flowtime.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.PowerManager
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
fun FlowTimeApp(
    flowTimeAppState: FlowTimeAppState = rememberAppState(),
    appTheme: AppTheme,
    showRating: Boolean,
    onThemeChanged: (AppTheme) -> Unit,
    onShowRatingChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity

    HideSystemBars()
    KeepScreenOn()

    if (showRating) {
        initiateReviewFlow(context, activity, onShowRatingChanged)
    }

    FlowTimeTheme(appTheme = appTheme) {
        FlowTimeNavHost(
            flowTimeAppState = flowTimeAppState,
            onThemeChanged = onThemeChanged
        )
    }
}

fun initiateReviewFlow(
    context: Context,
    activity: Activity,
    onShowRatingChanged: (Boolean) -> Unit
) {
    val manager: ReviewManager = ReviewManagerFactory.create(context)
    val request: Task<ReviewInfo> = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val reviewInfo: ReviewInfo = task.result
            val flow: Task<Void> = manager.launchReviewFlow(activity, reviewInfo)
            flow.addOnCompleteListener { _ ->
                onShowRatingChanged.invoke(false)
            }
        }
    }
}

@Composable
fun KeepScreenOn() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val window = context.findActivity()?.window ?: return
    val powerManager =
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp:WakeLockTag")

    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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