package com.baltajmn.flowtime.ui

import android.app.Activity
import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.R
import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.core.design.theme.FlowTimeTheme
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
fun FlowTimeApp(
    flowTimeAppState: FlowTimeAppState = rememberAppState(),
    appearance: Appearance,
    showOnBoard: Boolean,
    showRating: Boolean,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    rememberShowRating: Boolean,
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

    FlowTimeTheme(appearance = appearance) {
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
        containerColor = MaterialTheme.colorScheme.primaryContainer,
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
                        color = MaterialTheme.colorScheme.onPrimary
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
