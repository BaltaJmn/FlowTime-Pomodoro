package com.baltajmn.flowtime.features.screens.timer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.screen.TimerBaseScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun TimerScreen(
    mode: TimerMode,
    viewModel: TimerViewModel = koinViewModel(key = mode.name) { parametersOf(mode) }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var explainNotifications by remember { mutableStateOf(false) }
    val askNotifications = rememberLauncherForActivityResult(RequestPermission()) {}

    KeepScreenOn(active = state.keepScreenOn && state.isActive && !state.paused)

    TimerBaseScreen(
        state = state,
        title = title(state),
        onAction = { action ->
            viewModel.onAction(action)
            // La primera sesión es cuando se entiende para qué sirve el permiso.
            if (action == TimerAction.START && !notificationsAllowed(context)) {
                explainNotifications = viewModel.explainNotificationsOnce()
            }
        },
        onSwitchChanged = viewModel::changeSwitch,
        onTagSelected = viewModel::selectTag,
        onTaskSelected = viewModel::selectTask,
        onTaskDone = viewModel::completeTask,
        onTaskNotYet = viewModel::keepTask
    )

    if (explainNotifications) {
        AlertDialog(
            onDismissRequest = { explainNotifications = false },
            title = { Text(stringResource(R.string.notifications_title)) },
            text = { Text(stringResource(R.string.notifications_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explainNotifications = false
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                ) { Text(stringResource(R.string.turn_on)) }
            },
            dismissButton = {
                TextButton(onClick = { explainNotifications = false }) {
                    Text(stringResource(R.string.notifications_later))
                }
            }
        )
    }
}

// Antes de Android 13 no hay permiso que pedir; ContextCompat lo resuelve con los ajustes de la app.
private fun notificationsAllowed(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
    PackageManager.PERMISSION_GRANTED

@Composable
private fun title(state: TimerUiState): String = stringResource(
    when {
        state.paused -> R.string.time_title_paused
        state.phase == Phase.WORK -> R.string.time_title_working
        state.phase == Phase.BREAK -> R.string.time_title_resting
        state.mode == TimerMode.POMODORO -> R.string.pomodoro_title
        state.mode == TimerMode.FLOW_TIME -> R.string.flow_time_title
        else -> R.string.percentage_title
    }
)

/** Solo mientras cuenta y esta pantalla está a la vista: ya no hace falta para no perder el tiempo. */
@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}
