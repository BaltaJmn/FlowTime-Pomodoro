package com.baltajmn.flowtime.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.baltajmn.flowtime.R
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
    showOnBoard: Boolean,
    showRating: Boolean,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    rememberShowRating: Boolean,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit,
    onShowRatingChanged: (Boolean) -> Unit,
    onRememberShowRating: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val shouldShowRatingDialog = remember(showRating, rememberShowRating, showOnBoard) {
        showRating && rememberShowRating && !showOnBoard
    }

    var showDialog by remember(shouldShowRatingDialog) { mutableStateOf(shouldShowRatingDialog) }

    KeepScreenOn()

    FlowTimeTheme(appTheme = appTheme) {
        if (showDialog) {
            RatingDialog(
                onRateNow = {
                    activity?.let { act ->
                        initiateReviewFlow(context, act, onShowRatingChanged)
                    }
                    showDialog = false
                },
                onRemindLater = {
                    onShowRatingChanged(true)
                    showDialog = false
                },
                onNeverRemind = {
                    onRememberShowRating(false)
                    showDialog = false
                }
            )
        }

        FlowTimeNavHost(
            flowTimeAppState = flowTimeAppState,
            showSound = showSound,
            onSoundChange = onSoundChange,
            onThemeChanged = onThemeChanged,
            onSupportDeveloperClick = onSupportDeveloperClick
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
fun RatingDialog(
    onRateNow: () -> Unit,
    onRemindLater: () -> Unit,
    onNeverRemind: () -> Unit
) {
    AlertDialog(
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .border(
                2.dp,
                MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(28.dp)
            ),
        containerColor = MaterialTheme.colorScheme.secondary,
        titleContentColor = MaterialTheme.colorScheme.tertiary,
        textContentColor = MaterialTheme.colorScheme.tertiary,
        title = { Text(text = LocalContext.current.getString(R.string.rating_dialog_title)) },
        text = { Text(text = LocalContext.current.getString(R.string.rating_dialog_message)) },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = onRateNow) {
                    Text(
                        text = LocalContext.current.getString(R.string.rate_now),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        },
        dismissButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.clickable { onRemindLater.invoke() },
                    text = LocalContext.current.getString(R.string.remind_later),
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    modifier = Modifier.clickable { onNeverRemind.invoke() },
                    text = LocalContext.current.getString(R.string.never_remind),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        },
        onDismissRequest = { onNeverRemind.invoke() }
    )
}

@Composable
fun KeepScreenOn() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner, context) {
        val window = context.findActivity()?.window
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock =
            powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FlowTime:WakeLock")

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    try {
                        wakeLock?.acquire(10 * 60 * 1000L)
                    } catch (e: Exception) {
                        // Ignore wake lock errors
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    try {
                        if (wakeLock?.isHeld == true) {
                            wakeLock.release()
                        }
                    } catch (e: Exception) {
                        // Ignore wake lock errors
                    }
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                // Ignore wake lock errors
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