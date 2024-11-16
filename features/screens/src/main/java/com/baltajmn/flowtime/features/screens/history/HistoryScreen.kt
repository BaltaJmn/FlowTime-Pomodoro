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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

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
                allStudyTime = state.allStudyTime,
                onExportStudyTime = {
                    viewModel.exportStudyTime {
                        clipboardManager.setText(AnnotatedString(it))
                    }
                },
                onImportStudyTime = {
                    clipboardManager.getText()?.text?.let { text ->
                        viewModel.importStudyTime(text)
                    }
                }
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
    allMinutes: String,
    onExportStudyTime: () -> Unit,
    onImportStudyTime: () -> Unit
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
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LocalContext.current.getString(R.string.export),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onExportStudyTime
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(24.dp))

            Text(
                text = LocalContext.current.getString(R.string.import_button),
                color = MaterialTheme.colorScheme.primary
            )
            IconButton(
                onClick = onImportStudyTime
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun HistoryWeek(
    selectedDate: String,
    plusWeek: () -> Unit,
    minusWeek: () -> Unit,
    studyTime: List<Long>,
    allStudyTime: String,
    onExportStudyTime: () -> Unit,
    onImportStudyTime: () -> Unit
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

        BarChart(
            studyTime = studyTime,
            allStudyTime = allStudyTime,
            onExportStudyTime = onExportStudyTime,
            onImportStudyTime = onImportStudyTime
        )
    }
}

@Composable
fun BarChart(
    studyTime: List<Long>,
    allStudyTime: String,
    onExportStudyTime: () -> Unit,
    onImportStudyTime: () -> Unit
) {
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

        Spacer(modifier = Modifier.height(16.dp))

        AllMinutes(
            allMinutes = allStudyTime,
            onExportStudyTime = onExportStudyTime,
            onImportStudyTime = onImportStudyTime
        )

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
    }
}
