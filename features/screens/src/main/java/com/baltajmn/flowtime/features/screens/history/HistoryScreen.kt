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
import com.baltajmn.flowtime.core.common.extensions.capitalizeFirst
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudying
import com.baltajmn.flowtime.core.common.extensions.formatMinutesStudyingInHistory
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.Title
import org.koin.androidx.compose.koinViewModel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

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
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            HistoryWeek(
                selectedDate = state.selectedDateToShow,
                plusWeek = { viewModel.plusWeek() },
                minusWeek = { viewModel.minusWeek() },
                studyTime = state.studyTime,
                allStudyTime = state.allStudyTime
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
            modifier = Modifier.weight(0.1f),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )
        Text(
            modifier = Modifier.weight(0.9f),
            text = text,
            style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AllMinutes(
    allMinutes: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = LocalContext.current.getString(R.string.all_minutes_studying, allMinutes),
            textAlign = TextAlign.Center,
            style = SmallTitle,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun HistoryWeek(
    selectedDate: String,
    plusWeek: () -> Unit,
    minusWeek: () -> Unit,
    studyTime: List<Long>,
    allStudyTime: String
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
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = selectedDate,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )

            IconButton(onClick = { plusWeek.invoke() }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        BarChart(studyTime = studyTime, allStudyTime = allStudyTime)
    }
}

@Composable
fun BarChart(studyTime: List<Long>, allStudyTime: String) {
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
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
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
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = LocalContext.current.getString(R.string.total_minutes),
                textAlign = TextAlign.Center,
                style = Title,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = studyTime.sum().formatMinutesStudying(),
                textAlign = TextAlign.Center,
                style = SmallTitle,
                color = MaterialTheme.colorScheme.primary
            )
        }

        AllMinutes(allMinutes = allStudyTime)

        if (studyTime.sum() > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = LocalContext.current.getString(R.string.best_day),
                    textAlign = TextAlign.Center,
                    style = Title,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = DayOfWeek.entries[studyTime.indexOf(studyTime.maxOf { it })].getDisplayName(
                        TextStyle.FULL,
                        Locale.getDefault()
                    ).capitalizeFirst(),
                    textAlign = TextAlign.Center,
                    style = SmallTitle,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = LocalContext.current.getString(R.string.remember),
                textAlign = TextAlign.Center,
                style = Title,
                color = MaterialTheme.colorScheme.tertiary
            )
            Text(
                text = LocalContext.current.getString(
                    MotivationalPhrases.entries[
                        (MotivationalPhrases.entries.toTypedArray().indices).random()
                    ].resourceId
                ),
                textAlign = TextAlign.Center,
                style = SmallTitle,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

enum class MotivationalPhrases(val resourceId: Int) {
    MOT_1(R.string.mot_1),
    MOT_2(R.string.mot_2),
    MOT_3(R.string.mot_3),
    MOT_4(R.string.mot_4),
    MOT_5(R.string.mot_5),
    MOT_6(R.string.mot_6),
    MOT_7(R.string.mot_7),
    MOT_8(R.string.mot_8),
    MOT_9(R.string.mot_9),
    MOT_10(R.string.mot_10),
    MOT_11(R.string.mot_11),
    MOT_12(R.string.mot_12),
    MOT_13(R.string.mot_13),
    MOT_14(R.string.mot_14),
    MOT_15(R.string.mot_15),
    MOT_16(R.string.mot_16),
    MOT_17(R.string.mot_17),
    MOT_18(R.string.mot_18),
    MOT_19(R.string.mot_19),
    MOT_20(R.string.mot_20)
}
