package com.baltajmn.flowtime.features.screens.settings

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import android.annotation.SuppressLint
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.AppTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.core.design.theme.DarkMode
import com.baltajmn.flowtime.core.design.theme.LargeTitle
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.core.design.theme.Title
import com.baltajmn.flowtime.features.screens.settings.enum.MotivationalPhrases
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    listState: LazyListState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateToHistory: () -> Unit,
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
    onSupportDeveloperClick: () -> Unit
) {
    AnimatedContent(
        targetState = state.isLoading,
        label = "settings_loading"
    ) { isLoading ->
        if (isLoading) {
            LoadingView()
        } else {
            SettingsContent(
                state = state,
                listState = listState,
                viewModel = viewModel,
                showSound = showSound,
                onSoundChange = onSoundChange,
                navigateToHistory = navigateToHistory,
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
    onSupportDeveloperClick: () -> Unit
) {
    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item { Spacer(modifier = Modifier.height(80.dp)) }
        item {
            ProgressLevel(
                userLevel = state.userLevel,
                progressPercentage = state.progressPercentage
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            SupportButton(
                title = LocalContext.current.getString(R.string.support_developer_title),
                description = LocalContext.current.getString(R.string.support_developer_description),
                onSupportDeveloperClick = onSupportDeveloperClick
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            AppearanceCard(
                appearance = state.appearance,
                onDarkMode = viewModel::setDarkMode,
                onDynamicColor = viewModel::setDynamicColor,
                onTheme = viewModel::setTheme
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.others),
                        style = LargeTitle.copy(
                            fontSize = 30.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ButtonRow(
                        text = R.string.study_history,
                        button = R.string.go_to_history,
                        onClick = navigateToHistory
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CheckRow(
                        text = R.string.show_sound,
                        checked = showSound,
                        onCheckedChange = {
                            onSoundChange.invoke(it)
                            viewModel.saveSound(it)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CheckRow(
                        text = R.string.show_alert,
                        checked = state.showAlert,
                        onCheckedChange = viewModel::saveAlert
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    CheckRow(
                        text = R.string.keep_screen_on,
                        checked = state.keepScreenOn,
                        onCheckedChange = viewModel::saveKeepScreenOn
                    )

                    PermissionNotice(
                        text = R.string.notifications_off,
                        granted = { NotificationManagerCompat.from(it).areNotificationsEnabled() },
                        settings = {
                            Intent(ACTION_APP_NOTIFICATION_SETTINGS).putExtra(
                                EXTRA_APP_PACKAGE,
                                it.packageName
                            )
                        }
                    )

                    PermissionNotice(
                        text = R.string.exact_alarms_off,
                        granted = {
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                                it.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
                        },
                        settings = {
                            Intent(
                                ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${it.packageName}")
                            )
                        }
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { PositiveText() }
        item { Spacer(modifier = Modifier.height(192.dp)) }
    }
}

/** Solo se ve si falta un permiso. Se vuelve a mirar al volver de los ajustes del sistema. */
@Composable
fun PermissionNotice(
    @StringRes text: Int,
    granted: (Context) -> Boolean,
    settings: (Context) -> Intent
) {
    val context = LocalContext.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    if (remember(lifecycle) { granted(context) }) return
    Spacer(modifier = Modifier.height(8.dp))
    ButtonRow(text = text, button = R.string.turn_on) { context.startActivity(settings(context)) }
}

@Composable
fun ButtonRow(@StringRes text: Int, @StringRes button: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(text),
            style = SubBody.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        )
        Button(
            modifier = Modifier.weight(1f),
            onClick = onClick
        ) {
            Text(
                text = stringResource(button),
                style = SubBody.copy(color = MaterialTheme.colorScheme.onPrimary)
            )
        }
    }
}

@Composable
fun CheckRow(
    @StringRes text: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, end = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(text),
            style = SubBody.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
        )
        Checkbox(
            modifier = Modifier.weight(1f),
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun PositiveText() {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = LocalContext.current.getString(R.string.remember),
                textAlign = TextAlign.Center,
                style = Title,
                color = MaterialTheme.colorScheme.primary
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

@Composable
fun SupportButton(
    title: String,
    description: String,
    onSupportDeveloperClick: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = LargeTitle.copy(
                    fontSize = 25.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = SubBody.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onSupportDeveloperClick
            ) {
                Text(
                    text = LocalContext.current.getString(R.string.support_developer),
                    style = SubBody.copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ProgressLevel(
    userLevel: Long,
    progressPercentage: Long
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = LocalContext.current.getString(R.string.user_progression_level),
                style = LargeTitle.copy(fontSize = 26.sp, color = MaterialTheme.colorScheme.primary)
            )

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

            // El nivel va encima de la barra y no dentro: en blanco sobre la parte vacía no se leía.
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                text = stringResource(R.string.user_level_short, userLevel),
                style = SubBody.copy(
                    fontWeight = FontWeight.W700,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            BoxWithConstraints(
                Modifier
                    .padding(vertical = 8.dp)
                    .clip(RoundedCornerShape(30))
                    .border(1.dp, MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(30))
                    .height(24.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .animateContentSize()
                        .clip(RoundedCornerShape(30))
                        .height(24.dp)
                        .fillMaxWidth(fraction = width / 100)
                        .background(color = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

/** Claro u oscuro, los colores del fondo de pantalla (desde Android 12) y el tema. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceCard(
    appearance: Appearance,
    onDarkMode: (DarkMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onTheme: (AppTheme) -> Unit
) {
    val canUseWallpaper = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    // Con los colores del fondo de pantalla, el tema no se usa: se ve, pero apagado.
    val themesEnabled = !(canUseWallpaper && appearance.dynamicColor)

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.appearance_title),
                style = LargeTitle.copy(
                    fontSize = 30.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                DarkMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = appearance.darkMode == mode,
                        onClick = { onDarkMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, DarkMode.entries.size)
                    ) {
                        Text(text = stringResource(mode.label), style = SubBody)
                    }
                }
            }

            if (canUseWallpaper) {
                CheckRow(
                    text = R.string.appearance_dynamic,
                    checked = appearance.dynamicColor,
                    onCheckedChange = onDynamicColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (themesEnabled) 1f else 0.38f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(AppTheme.entries) { theme ->
                    ThemeSwatch(
                        theme = theme,
                        selected = theme == appearance.theme,
                        enabled = themesEnabled,
                        onClick = { onTheme(theme) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val description = stringResource(R.string.cd_theme_color, stringResource(theme.label))
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(theme.color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                shape = CircleShape
            )
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            // Sobre el color de muestra, no sobre el tema: blanco o negro según lo claro que sea.
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (theme.color.luminance() > 0.5f) Color.Black else Color.White
            )
        }
    }
}
