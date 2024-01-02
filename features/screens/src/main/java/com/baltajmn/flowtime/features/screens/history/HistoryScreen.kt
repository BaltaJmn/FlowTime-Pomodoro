package com.baltajmn.flowtime.features.screens.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudyingInHistory
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.Title
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = koinViewModel(),
    navigateUp: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedHistoryContent(
        state = state,
        viewModel = viewModel,
        navigateUp = navigateUp
    )
}

@Composable
fun AnimatedHistoryContent(
    state: HistoryState,
    viewModel: HistoryViewModel,
    navigateUp: () -> Unit
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) {
        when (it) {
            true -> LoadingView()
            false -> HistoryContent(
                state = state,
                viewModel = viewModel,
                navigateUp = navigateUp
            )
        }
    }
}

@Composable
fun HistoryContent(
    state: HistoryState,
    viewModel: HistoryViewModel,
    navigateUp: () -> Unit
) {
    LazyColumn(
        state = rememberLazyListState(),
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            ScreenTitleWithBack(
                text = LocalContext.current.getString(R.string.study_history),
                navigateUp = navigateUp
            )
        }
        item { Spacer(modifier = Modifier.height(64.dp)) }
        item {
            HistoryWeek(
                selectedDate = state.selectedDate,
                plusWeek = { viewModel.plusWeek() },
                minusWeek = { viewModel.minusWeek() },
                studyTime = state.studyTime
            )
        }
    }
}

@Composable
fun ScreenTitleWithBack(text: String, navigateUp: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { navigateUp.invoke() },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier
                .width(50.dp)
                .size(40.dp),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )
        Text(
            text = text,
            style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun HistoryWeek(
    selectedDate: LocalDate,
    plusWeek: () -> Unit,
    minusWeek: () -> Unit,
    studyTime: List<Long>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = { minusWeek.invoke() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null)
            }

            Text(
                text = buildWeekLabel(selectedDate),
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                textAlign = TextAlign.Center
            )

            IconButton(onClick = { plusWeek.invoke() }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }

        BarChart(studyTime)
    }
}

@Composable
fun BarChart(studyTime: List<Long>) {
    val daysOfWeek: List<String> = DayOfWeek.entries
        .map { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
        .toList()
    val maxHours = studyTime.maxOrNull() ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            for (time in studyTime) {
                Text(
                    text = time.formatMinutesStudyingInHistory(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in daysOfWeek.indices) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 8.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .height(((studyTime[i] / maxHours.toFloat()) * 200).dp)
                            .padding(4.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            for (day in daysOfWeek) {
                Text(
                    text = day,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
        ) {
            Text(
                text = "Total minutes: ${studyTime.sum().formatMinutesStudying()}",
                textAlign = TextAlign.Center,
                style = Title,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun buildWeekLabel(date: LocalDate): String {
    val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val endOfWeek = startOfWeek.plusDays(6)
    val startMonth = startOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val endMonth = endOfWeek.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    return "${startOfWeek.dayOfMonth} $startMonth - ${endOfWeek.dayOfMonth} $endMonth"
}