package com.baltajmn.flowtime.features.screens.settings

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.core.persistence.model.RangeModel
import com.baltajmn.flowtime.features.screens.components.FlowTimeRanges
import com.baltajmn.flowtime.features.screens.components.PercentageRange
import com.baltajmn.flowtime.features.screens.components.PomodoroRange
import com.baltajmn.flowtime.features.screens.settings.enum.MotivationalPhrases
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    listState: LazyListState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateToHistory: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedSettingsContent(
        state = state,
        listState = listState,
        viewModel = viewModel,
        showSound = showSound,
        onSoundChange = onSoundChange,
        navigateToHistory = navigateToHistory,
        onThemeChanged = onThemeChanged,
        onSupportDeveloperClick = onSupportDeveloperClick
    )
}

@Composable
fun AnimatedSettingsContent(
    state: SettingsState,
    listState: LazyListState,
    viewModel: SettingsViewModel,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateToHistory: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = ""
    ) {
        when (it) {
            true -> LoadingView()
            false -> SettingsContent(
                state = state,
                listState = listState,
                viewModel = viewModel,
                showSound = showSound,
                onSoundChange = onSoundChange,
                navigateToHistory = navigateToHistory,
                onThemeChanged = onThemeChanged,
                onSupportDeveloperClick = onSupportDeveloperClick
            )
        }
    }
}

@Composable
fun SettingsContent(
    state: SettingsState,
    listState: LazyListState,
    viewModel: SettingsViewModel,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateToHistory: () -> Unit,
    onThemeChanged: (AppTheme) -> Unit,
    onSupportDeveloperClick: () -> Unit
) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
    ) {
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            SupportButton(
                title = LocalContext.current.getString(R.string.support_developer_title),
                description = LocalContext.current.getString(R.string.support_developer_description),
                onSupportDeveloperClick = onSupportDeveloperClick
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            ProgressLevel(
                userLevel = state.userLevel,
                progressPercentage = state.progressPercentage
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.flow_time_settings_title),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        FlowTimeRanges(
            ranges = state.flowTimeRanges,
            onValueChanged = { index: Int, range: RangeModel ->
                viewModel.modifyRange(
                    index = index,
                    range = range
                )
            },
            onDeleteClicked = { index: Int -> viewModel.deleteRange(index = index) },
            onAddRangeClicked = { viewModel.addRange() }
        )
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.pomodoro_settings_title),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        item {
            PomodoroRange(
                range = state.pomodoroRange,
                onValueChanged = { range: RangeModel ->
                    viewModel.modifyPomodoro(range)
                }
            )
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.percentage_settings_title),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        item {
            PercentageRange(
                percentage = state.percentage,
                onPercentageChange = { percentage ->
                    viewModel.modifyPercentage(percentage)
                }
            )
        }
        item { Spacer(modifier = Modifier.height(32.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.theme_settings_title),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            LazyRow(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(AppTheme.entries) { color ->
                    Box(
                        modifier = Modifier
                            .background(color = color.color)
                            .aspectRatio(1f)
                            .size(32.dp)
                            .semantics { contentDescription = "Color $color" }
                            .clickable {
                                onThemeChanged.invoke(color)
                                viewModel.saveColor(color)
                            }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            ButtonSave {
                viewModel.saveChanges()
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(
                text = LocalContext.current.getString(R.string.others),
                style = LargeTitle.copy(fontSize = 30.sp, color = MaterialTheme.colorScheme.primary)
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item { ButtonHistory(navigateToHistory = navigateToHistory) }
        item { Spacer(modifier = Modifier.height(8.dp)) }
        item {
            ButtonSound(
                showSound = showSound,
                onCheckedChange = {
                    onSoundChange.invoke(it)
                    viewModel.saveSound(it)
                }
            )
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { PositiveText() }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

@Composable
fun ButtonSave(onSaveClicked: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { onSaveClicked.invoke() }) {
            Text(
                text = LocalContext.current.getString(R.string.settings_save),
                style = SubBody.copy(color = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
fun ButtonHistory(navigateToHistory: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = LocalContext.current.getString((R.string.study_history)),
            style = SubBody.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = { navigateToHistory.invoke() }
        ) {
            Text(
                text = LocalContext.current.getString(R.string.go_to_history),
                style = SubBody.copy(color = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@Composable
fun ButtonSound(
    showSound: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val hasToShow by rememberSaveable(showSound) {
        mutableStateOf(showSound)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = LocalContext.current.getString((R.string.show_sound)),
            style = SubBody.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        )
        Checkbox(
            modifier = Modifier.weight(1f),
            checked = hasToShow,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PositiveText() {
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

@Composable
fun SupportButton(
    title: String,
    description: String,
    onSupportDeveloperClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = LargeTitle.copy(fontSize = 25.sp, color = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = SubBody.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSupportDeveloperClick
        ) {
            Text(
                text = LocalContext.current.getString(R.string.support_developer),
                style = SubBody.copy(color = MaterialTheme.colorScheme.secondary)
            )
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ProgressLevel(
    userLevel: Long,
    progressPercentage: Long
) {
    Text(
        text = LocalContext.current.getString(R.string.user_progression_level),
        style = LargeTitle.copy(fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        var animateWidth by rememberSaveable {
            mutableStateOf(false)
        }

        LaunchedEffect(key1 = Unit) {
            if (animateWidth) return@LaunchedEffect
            animateWidth = true
        }

        val width by animateFloatAsState(
            if (animateWidth) progressPercentage.toFloat() else 0f,
            label = "",
            animationSpec = tween(
                durationMillis = 1000,
                delayMillis = 100,
                easing = LinearOutSlowInEasing
            )
        )

        BoxWithConstraints(
            Modifier
                .padding(vertical = 8.dp)
                .clip(RoundedCornerShape(30))
                .border(1.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(30))
                .height(24.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.tertiary)
        ) {
            Box(
                modifier = Modifier
                    .animateContentSize()
                    .clip(RoundedCornerShape(30))
                    .height(24.dp)
                    .fillMaxWidth(fraction = width / 100)
                    .background(color = MaterialTheme.colorScheme.secondary)
            )
            Text(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterStart),
                text = "Lvl. $userLevel",
                style = SubBody.copy(
                    fontWeight = FontWeight.W700,
                    color = if (progressPercentage.toInt() == 0) {
                        MaterialTheme.colorScheme.secondary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            )
        }
    }
}
