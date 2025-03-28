package com.baltajmn.flowtime.features.screens.percentage

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.CircularButton
import com.baltajmn.flowtime.core.design.components.ComposableLifecycle
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.features.screens.components.MinutesStudying
import com.baltajmn.flowtime.features.screens.components.ScreenTitle
import com.baltajmn.flowtime.features.screens.components.TimeContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun PercentageScreen(
    viewModel: PercentageViewModel = koinViewModel(),
    listState: LazyListState,
    onTimerRunning: (Boolean) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.getPercentageConfig()
                viewModel.getCurrentMinutes()
            }

            else -> {}
        }
    }

    onTimerRunning.invoke((state.isTimerRunning || state.isBreakRunning))

    AnimatedPercentageContent(
        state = state,
        listState = listState,
        onStartClick = { viewModel.startTimer() },
        onBreakClick = { viewModel.continueWithBreak() },
        onFinishClick = { viewModel.stopTimer() },
        onSwitchChanged = { viewModel.changeSwitch(it) }
    )
}

@Composable
fun AnimatedPercentageContent(
    state: PercentageState,
    listState: LazyListState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSwitchChanged: (Boolean) -> Unit
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) {
        when (it) {
            true -> LoadingView()
            false -> PercentageContent(
                state = state,
                listState = listState,
                onStartClick = onStartClick,
                onBreakClick = onBreakClick,
                onFinishClick = onFinishClick,
                onSwitchChanged = onSwitchChanged
            )
        }
    }
}

@Composable
fun PercentageContent(
    state: PercentageState,
    listState: LazyListState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSwitchChanged: (Boolean) -> Unit
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeContent(
            state = state,
            onStartClick = onStartClick,
            onBreakClick = onBreakClick,
            onFinishClick = onFinishClick,
            onSwitchChanged = onSwitchChanged
        )
    } else {
        PortraitContent(
            state = state,
            onStartClick = onStartClick,
            onBreakClick = onBreakClick,
            onFinishClick = onFinishClick,
            onSwitchChanged = onSwitchChanged
        )
    }
}

@Composable
fun PortraitContent(
    state: PercentageState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSwitchChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(PaddingValues(bottom = 120.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenTitle(
            text = if (state.isTimerRunning && state.isBreakRunning.not()) {
                LocalContext.current.getString(R.string.time_title_working)
            } else if (state.isTimerRunning.not() && state.isBreakRunning) {
                LocalContext.current.getString(R.string.time_title_resting)
            } else {
                LocalContext.current.getString(R.string.percentage_title)
            }
        )
        Spacer(modifier = Modifier.height(32.dp))
        TimeContent(secondsFormatted = state.secondsFormatted)
        Spacer(modifier = Modifier.height(16.dp))
        MinutesStudying(minutesStudying = state.minutesStudying)
        Spacer(modifier = Modifier.height(64.dp))
        ButtonsContent(
            state = state,
            onStartClick = onStartClick,
            onBreakClick = onBreakClick,
            onFinishClick = onFinishClick
        )
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = LocalContext.current.getString(R.string.pomodoro_continue_after_break),
            style = Title.copy(
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        )
        Switch(
            checked = state.continueAfterBreak,
            onCheckedChange = { onSwitchChanged.invoke(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                checkedBorderColor = MaterialTheme.colorScheme.tertiary,
                uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                uncheckedTrackColor = MaterialTheme.colorScheme.secondary,
                uncheckedBorderColor = MaterialTheme.colorScheme.tertiary

            )
        )
    }
}

@Composable
fun LandscapeContent(
    state: PercentageState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
    onSwitchChanged: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (state.isBreakRunning.not() && state.isTimerRunning.not()) {
                        LocalContext.current.getString(R.string.percentage_title)
                    } else if (state.isBreakRunning) {
                        LocalContext.current.getString(R.string.time_title_resting)
                    } else {
                        LocalContext.current.getString(R.string.time_title_working)
                    },
                    style = LargeTitle.copy(
                        fontSize = 30.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                TimeContent(secondsFormatted = state.secondsFormatted)
                Spacer(modifier = Modifier.height(8.dp))
                MinutesStudying(minutesStudying = state.minutesStudying)
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(0.3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ButtonsContentLandscape(
                    state = state,
                    onStartClick = onStartClick,
                    onBreakClick = onBreakClick,
                    onFinishClick = onFinishClick
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = LocalContext.current.getString(R.string.pomodoro_continue_after_break),
                    style = Title.copy(
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                )
                Switch(
                    checked = state.continueAfterBreak,
                    onCheckedChange = { onSwitchChanged.invoke(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.tertiary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedBorderColor = MaterialTheme.colorScheme.tertiary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.tertiary,
                        uncheckedTrackColor = MaterialTheme.colorScheme.secondary,
                        uncheckedBorderColor = MaterialTheme.colorScheme.tertiary

                    )
                )
            }
        }
    }
}

@Composable
fun ButtonsContent(
    state: PercentageState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {
            CircularButton(
                onClick = { onFinishClick.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            if (state.isBreakRunning.not()) {
                CircularButton(
                    onClick = { onBreakClick.invoke() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        } else {
            CircularButton(
                onClick = { onStartClick.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun ButtonsContentLandscape(
    state: PercentageState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {
            CircularButton(
                onClick = { onFinishClick.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }

            if (state.isBreakRunning.not()) {
                Spacer(modifier = Modifier.height(64.dp))

                CircularButton(
                    onClick = { onBreakClick.invoke() }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_next),
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        } else {
            CircularButton(
                onClick = { onStartClick.invoke() }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}