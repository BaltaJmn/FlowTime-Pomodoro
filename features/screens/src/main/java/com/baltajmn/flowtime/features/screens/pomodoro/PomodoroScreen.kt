package com.baltajmn.flowtime.features.screens.pomodoro

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.baltajmn.flowtime.features.screens.components.MinutesStudying
import com.baltajmn.flowtime.features.screens.components.ScreenTitle
import com.baltajmn.flowtime.features.screens.components.TimeContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun PomodoroScreen(
    viewModel: PomodoroViewModel = koinViewModel(),
    listState: LazyListState,
    onTimerRunning: (Boolean) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ComposableLifecycle { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> {
                viewModel.getPomodoroConfig()
                viewModel.getCurrentMinutes()
            }

            Lifecycle.Event.ON_PAUSE -> {
                viewModel.stopTimer()
            }

            else -> {}
        }
    }

    onTimerRunning.invoke((state.isTimerRunning || state.isBreakRunning))

    AnimatedPomodoroContent(
        state = state,
        listState = listState,
        onStartClick = { viewModel.startTimer() },
        onFinishClick = { viewModel.stopTimer() },
    )
}

@Composable
fun AnimatedPomodoroContent(
    state: PomodoroState,
    listState: LazyListState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) {
        when (it) {
            true -> LoadingView()
            false -> PomodoroContent(
                state = state,
                listState = listState,
                onStartClick = onStartClick,
                onFinishClick = onFinishClick,
            )
        }
    }
}

@Composable
fun PomodoroContent(
    state: PomodoroState,
    listState: LazyListState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeContent(
            state = state,
            onStartClick = onStartClick,
            onFinishClick = onFinishClick
        )
    } else {
        PortraitContent(
            state = state,
            onStartClick = onStartClick,
            onFinishClick = onFinishClick
        )
    }
}

@Composable
fun LandscapeContent(
    state: PomodoroState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
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
                        LocalContext.current.getString(R.string.pomodoro_title)
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
                    onFinishClick = onFinishClick,
                )
            }
        }
    }
}

@Composable
fun ButtonsContentLandscape(
    state: PomodoroState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {
            CircularButton(
                onClick = { onFinishClick.invoke() },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        } else {
            CircularButton(
                onClick = { onStartClick.invoke() },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
fun PortraitContent(
    state: PomodoroState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
            .padding(PaddingValues(bottom = 120.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScreenTitle(
            text = if (state.isBreakRunning.not() && state.isTimerRunning.not()) {
                LocalContext.current.getString(R.string.pomodoro_title)
            } else if (state.isBreakRunning) {
                LocalContext.current.getString(R.string.time_title_resting)
            } else {
                LocalContext.current.getString(R.string.time_title_working)
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
            onFinishClick = onFinishClick,
        )
    }
}

@Composable
fun ButtonsContent(
    state: PomodoroState,
    onStartClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.isTimerRunning || state.isBreakRunning) {
            CircularButton(
                onClick = { onFinishClick.invoke() },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stop),
                    contentDescription = "Clear",
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        } else {
            CircularButton(
                onClick = { onStartClick.invoke() },
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_play),
                    contentDescription = "Add",
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}