package com.baltajmn.flowtime.features.screens.flowtime

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.features.screens.components.ButtonsContent
import com.baltajmn.flowtime.features.screens.components.ButtonsContentLandscape
import com.baltajmn.flowtime.features.screens.components.TimeContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun FlowTimeScreen(
    viewModel: FlowTimeViewModel = koinViewModel(),
    listState: LazyListState
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedFlowTimeContent(
        state = state,
        listState = listState,
        onStartClick = { viewModel.startTimer() },
        onBreakClick = { viewModel.continueWithBreak() },
        onFinishClick = { viewModel.stopTimer() },
    )
}

@Composable
fun AnimatedFlowTimeContent(
    state: FlowTimeState,
    listState: LazyListState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) {
        when (it) {
            true -> LoadingView()
            false -> FLowTimeContent(
                state = state,
                listState = listState,
                onStartClick = onStartClick,
                onBreakClick = onBreakClick,
                onFinishClick = onFinishClick,
            )
        }
    }
}

@Composable
fun FLowTimeContent(
    state: FlowTimeState,
    listState: LazyListState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
    onFinishClick: () -> Unit,
) {
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeContent(
            state = state,
            onStartClick = onStartClick,
            onBreakClick = onBreakClick,
            onFinishClick = onFinishClick
        )
    } else {
        PortraitContent(
            state = state,
            onStartClick = onStartClick,
            onBreakClick = onBreakClick,
            onFinishClick = onFinishClick
        )
    }
}

@Composable
fun PortraitContent(
    state: FlowTimeState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
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
        Text(
            text = if (state.isBreakRunning.not() && state.isTimerRunning.not()) {
                LocalContext.current.getString(R.string.flow_time_title)
            } else if (state.isBreakRunning) {
                LocalContext.current.getString(R.string.time_title_resting)
            } else {
                LocalContext.current.getString(R.string.time_title_working)
            },
            style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(32.dp))
        TimeContent(secondsFormatted = state.secondsFormatted)
        Spacer(modifier = Modifier.height(64.dp))
        ButtonsContent(
            state = state,
            onStartClick = onStartClick,
            onBreakClick = onBreakClick,
            onFinishClick = onFinishClick,
        )
    }
}

@Composable
fun LandscapeContent(
    state: FlowTimeState,
    onStartClick: () -> Unit,
    onBreakClick: () -> Unit,
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
                        LocalContext.current.getString(R.string.flow_time_title)
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
                    onBreakClick = onBreakClick,
                    onFinishClick = onFinishClick,
                )
            }
        }
    }
}

@Preview
@Composable
fun TimeContentPreview() {
    TimeContent(secondsFormatted = "44:44:44")
}