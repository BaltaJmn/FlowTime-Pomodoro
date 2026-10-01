package com.baltajmn.flowtime.features.screens.onboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.components.collectEvents
import com.baltajmn.flowtime.core.design.components.quietSegmentedColors
import com.baltajmn.flowtime.core.design.extensions.readableWidth
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.data.timer.TimerMode
import com.baltajmn.flowtime.features.screens.common.composable.components.advantages
import com.baltajmn.flowtime.features.screens.common.composable.components.label
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.Back
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel.Event.NavigateToMainGraph
import com.baltajmn.flowtime.features.screens.settings.GoalStepper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnBoardScreen(
    viewModel: OnBoardViewModel = koinViewModel(),
    navigateToMainGraph: () -> Unit,
    navigateBack: () -> Unit
) {
    collectEvents {
        viewModel.event.collectLatest {
            when (it) {
                NavigateToMainGraph -> navigateToMainGraph()
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

/** Qué es FlowTime, el modo y el objetivo, y empezar. "Saltar" termina con los valores de partida. */
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
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(16.dp)
            .readableWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> OnBoardItem(
                    imageRes = Res.drawable.ic_flowtime,
                    title = Res.string.on_board_intro_title,
                    description = Res.string.on_board_intro_text
                )
                1 -> SetupPage(state = state, onMode = onMode, onGoal = onGoal)
                else -> OnBoardItem(
                    imageRes = Res.drawable.ic_play,
                    title = Res.string.on_board_ready_title,
                    description = Res.string.on_board_ready_text
                )
            }
        }

        // Los puntos van en el centro de verdad, midan lo que midan los botones en cada idioma.
        Box(modifier = Modifier.fillMaxWidth()) {
            // En la última sobra: "Saltar" olvidaría el modo y el objetivo recién elegidos.
            if (!last) {
                TextButton(
                    onClick = onSkip,
                    modifier = Modifier.align(Alignment.CenterStart),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(text = stringResource(Res.string.on_board_skip))
                }
            }
            PageDots(current = pagerState.currentPage, modifier = Modifier.align(Alignment.Center))
            Button(
                onClick = {
                    if (last) {
                        onStart()
                    } else {
                        val next = pagerState.currentPage + 1
                        coroutineScope.launch { pagerState.animateScrollToPage(next) }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Text(text = stringResource(if (last) Res.string.on_board_start else Res.string.on_board_next))
            }
        }
    }
}

/** Un punto por página; el de la actual, alargado y en el acento. */
@Composable
private fun PageDots(current: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(PAGES) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 20.dp else 6.dp)
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            )
            Box(
                modifier = Modifier
                    .size(width = width, height = 6.dp)
                    .background(color, CircleShape)
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
        Text(text = stringResource(Res.string.on_board_setup_title), style = pageTitle())
        Spacer(modifier = Modifier.height(28.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = TimerMode.entries
            modes.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = state.mode == mode,
                    onClick = { onMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    colors = quietSegmentedColors(),
                    enabled = !state.modeLocked,
                    // Como en la pantalla de concentración: sin la marca, "Porcentaje" cabe.
                    icon = {},
                    label = {
                        Text(text = stringResource(mode.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Tres líneas siempre: al cambiar de modo, el resto de la página no da un salto.
        Text(
            text = stringResource(state.mode.advantages),
            modifier = Modifier.padding(horizontal = 8.dp),
            minLines = 3,
            style = SubBody.copy(textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(Res.string.goal_title),
            style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        Spacer(modifier = Modifier.height(8.dp))
        GoalStepper(minutes = state.goalMinutes, onChange = onGoal)
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(Res.string.on_board_setup_later),
            style = SubBody.copy(
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun OnBoardItem(
    imageRes: DrawableResource,
    title: StringResource,
    description: StringResource
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pequeño: lo que se lee es el texto. Y menos aún si no cabe (un móvil en horizontal).
        Image(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(bottom = 32.dp)
                .sizeIn(maxWidth = 96.dp, maxHeight = 96.dp)
                .aspectRatio(1f),
            painter = painterResource(imageRes),
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            contentDescription = null
        )
        Text(text = stringResource(title), style = pageTitle())
        Text(
            text = stringResource(description),
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp),
            style = SubBody.copy(
                fontSize = 17.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun pageTitle(): TextStyle =
    LargeTitle.copy(fontSize = 30.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
