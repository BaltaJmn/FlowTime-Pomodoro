package com.baltajmn.flowtime.features.screens.onboard

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.collectEvents
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.Back
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.NavigateToMainGraph
import com.baltajmn.flowtime.features.screens.settings.GoalStepper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnBoardScreen(
    viewModel: OnBoardViewModel = koinViewModel(),
    navigateToMainGraph: (timer: TimerMode?) -> Unit,
    navigateBack: () -> Unit
) {
    collectEvents {
        viewModel.event.collectLatest {
            when (it) {
                is NavigateToMainGraph -> navigateToMainGraph(it.timer)
                Back -> navigateBack()
            }
        }
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingContent(
        state = state,
        onMode = viewModel::selectMode,
        onGoal = viewModel::changeGoal,
        onSkip = viewModel::skip,
        onStart = viewModel::start
    )
}

private const val PAGES = 3

/** Qué es FlowTime, el modo y el objetivo, y empezar. "Saltar" termina desde cualquier página. */
@Composable
fun OnboardingContent(
    state: OnBoardUiState,
    onMode: (TimerMode) -> Unit,
    onGoal: (delta: Int) -> Unit,
    onSkip: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { PAGES })
    val coroutineScope = rememberCoroutineScope()
    val last = pagerState.currentPage == PAGES - 1

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> OnBoardItem(
                    imageRes = R.drawable.ic_flowtime,
                    title = R.string.on_board_intro_title,
                    description = R.string.on_board_intro_text
                )
                1 -> SetupPage(state = state, onMode = onMode, onGoal = onGoal)
                else -> OnBoardItem(
                    imageRes = R.drawable.ic_play,
                    title = R.string.on_board_ready_title,
                    description = R.string.on_board_ready_text
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                modifier = Modifier.clickable(onClick = onSkip),
                text = stringResource(R.string.on_board_skip),
                style = Title.copy(fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                repeat(PAGES) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .width(if (isSelected) 18.dp else 8.dp)
                            .height(8.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.tertiary,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.secondary
                                },
                                shape = CircleShape
                            )
                    )
                }
            }

            Text(
                modifier = Modifier.clickable {
                    if (last) {
                        onStart()
                    } else {
                        val next = pagerState.currentPage + 1
                        coroutineScope.launch { pagerState.animateScrollToPage(next) }
                    }
                },
                text = stringResource(if (last) R.string.on_board_start else R.string.on_board_next),
                style = Title.copy(fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupPage(
    state: OnBoardUiState,
    onMode: (TimerMode) -> Unit,
    onGoal: (delta: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.on_board_setup_title),
            style = LargeTitle.copy(
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.tertiary
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = TimerMode.entries
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { onMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    enabled = !state.modeLocked,
                    label = { Text(text = stringResource(mode.label), maxLines = 1) }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(state.mode.advantages),
            modifier = Modifier.padding(horizontal = 8.dp),
            style = SubBody.copy(
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(R.string.goal_title),
            style = LargeTitle.copy(fontSize = 20.sp, color = MaterialTheme.colorScheme.tertiary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        GoalStepper(minutes = state.goalMinutes, onChange = onGoal)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.on_board_setup_later),
            style = SubBody.copy(
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun OnBoardItem(
    @DrawableRes imageRes: Int,
    @StringRes title: Int,
    @StringRes description: Int
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            modifier = Modifier
                .height(350.dp)
                .width(350.dp)
                .padding(bottom = 20.dp),
            painter = painterResource(id = imageRes),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.tertiary),
            contentDescription = null
        )
        Text(
            text = stringResource(title),
            style = LargeTitle.copy(
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.tertiary
            )
        )
        Text(
            text = stringResource(description),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            style = Title.copy(
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private val TimerMode.label
    get() = when (this) {
        TimerMode.POMODORO -> R.string.mode_pomodoro
        TimerMode.FLOW_TIME -> R.string.mode_flow_time
        TimerMode.PERCENTAGE -> R.string.mode_percentage
    }

private val TimerMode.advantages
    get() = when (this) {
        TimerMode.POMODORO -> R.string.pomodoro_advantages
        TimerMode.FLOW_TIME -> R.string.flow_time_advantages
        TimerMode.PERCENTAGE -> R.string.percentage_advantages
    }
