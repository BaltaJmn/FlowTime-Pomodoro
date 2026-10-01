package com.baltajmn.flowtime.features.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import com.baltajmn.flowtime.core.design.components.FlowCard
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.baltajmn.flowtime.features.screens.common.composable.components.ScreenTitle
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Switch
import androidx.compose.ui.semantics.heading
import com.baltajmn.flowtime.core.design.components.quietSegmentedColors
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
// Los nombres de los temas y del modo oscuro vienen de core/design: StringResource.
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.baltajmn.flowtime.core.design.extensions.readableWidth
import com.baltajmn.flowtime.core.design.components.LoadingView
import com.baltajmn.flowtime.core.design.theme.AppTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
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
import com.baltajmn.flowtime.core.design.theme.SmallTitle
import com.baltajmn.flowtime.core.design.theme.SubBody
import com.baltajmn.flowtime.features.screens.settings.enum.MotivationalPhrases
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import com.baltajmn.flowtime.features.screens.pro.ProFeature
import com.baltajmn.flowtime.features.screens.pro.ProLauncher
import com.baltajmn.flowtime.features.screens.support.SupportSheet
import org.koin.compose.viewmodel.koinViewModel
import org.koin.compose.koinInject
import com.baltajmn.flowtime.core.design.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.getString
import com.baltajmn.flowtime.features.screens.platform.rememberSystemSettings
import com.baltajmn.flowtime.features.screens.platform.rememberShowMessage
import com.baltajmn.flowtime.features.screens.platform.hasWallpaperColors
import com.baltajmn.flowtime.features.screens.platform.hasFocusMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.derivedStateOf
import com.baltajmn.flowtime.data.pro.ProFeatures

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    listState: LazyListState,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateToIntro: () -> Unit,
    /** Pide al sistema poner el botón en los ajustes rápidos; null antes de Android 13. */
    onAddQuickTile: (() -> Unit)? = null,
    proLauncher: ProLauncher = koinInject()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnimatedSettingsContent(
        state = state,
        listState = listState,
        viewModel = viewModel,
        showSound = showSound,
        onSoundChange = onSoundChange,
        navigateToIntro = navigateToIntro,
        onAddQuickTile = onAddQuickTile,
        onOpenPro = proLauncher::open
    )
}

@Composable
fun AnimatedSettingsContent(
    state: SettingsState,
    listState: LazyListState,
    viewModel: SettingsViewModel,
    showSound: Boolean,
    onSoundChange: (Boolean) -> Unit,
    navigateToIntro: () -> Unit,
    onAddQuickTile: (() -> Unit)?,
    onOpenPro: (ProFeature?) -> Unit
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
                navigateToIntro = navigateToIntro,
                onAddQuickTile = onAddQuickTile,
                onOpenPro = onOpenPro
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
    navigateToIntro: () -> Unit,
    onOpenPro: (ProFeature?) -> Unit,
    onAddQuickTile: (() -> Unit)? = null
) {
    var showSupport by rememberSaveable { mutableStateOf(false) }
    if (showSupport) SupportSheet(onDismiss = { showSupport = false })

    LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.Top,
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .readableWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        item { ScreenTitle(text = stringResource(Res.string.nav_settings)) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { GoalCard(goal = state.goal, onChange = viewModel::changeGoal) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { ReminderCard(reminder = state.reminder, onChange = viewModel::setReminder) }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            TagsCard(
                state = state.tags,
                onAdd = viewModel::addTag,
                onRename = viewModel::renameTag,
                onRecolor = viewModel::recolorTag,
                onArchive = viewModel::archiveTag,
                onUnarchive = viewModel::unarchiveTag,
                onMessageShown = viewModel::onTagMessageShown,
                onSeePro = { onOpenPro(ProFeature.TAGS) }
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        if (state.purchases.pro != ProAccess.HIDDEN) {
            item {
                ProCard(
                    owned = state.purchases.pro == ProAccess.OPEN,
                    onOpen = { onOpenPro(null) }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
        if (ProFeatures.hasStore) {
            item {
                SupportCard(
                    state = state.purchases,
                    onTip = { showSupport = true },
                    onRestore = viewModel::restorePurchases,
                    onMessageShown = viewModel::onRestoreMessageShown
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
        item {
            AppearanceCard(
                appearance = state.appearance,
                isSupporter = state.purchases.isSupporter,
                onDarkMode = viewModel::setDarkMode,
                onDynamicColor = viewModel::setDynamicColor,
                onTheme = viewModel::setTheme,
                pro = state.purchases.pro,
                onLockedTheme = { onOpenPro(ProFeature.THEMES) },
                icon = state.appIcon,
                onIcon = viewModel::setAppIcon
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            SettingsCard(Res.string.others) {
                Spacer(modifier = Modifier.height(4.dp))

                ButtonRow(
                    text = Res.string.settings_intro,
                    button = Res.string.settings_intro_button,
                    onClick = navigateToIntro
                )

                val system = rememberSystemSettings()
                ButtonRow(
                    text = Res.string.settings_rate,
                    button = Res.string.settings_rate_button,
                    onClick = system::openStoreListing
                )

                val uriHandler = LocalUriHandler.current
                ButtonRow(
                    text = Res.string.settings_privacy,
                    button = Res.string.settings_intro_button,
                    onClick = { runCatching { uriHandler.openUri(PRIVACY_URL) } }
                )

                onAddQuickTile?.let {
                    ButtonRow(
                        text = Res.string.settings_quick_tile,
                        button = Res.string.settings_quick_tile_button,
                        onClick = it
                    )
                }

                SwitchRow(
                    text = Res.string.show_sound,
                    checked = showSound,
                    onCheckedChange = {
                        onSoundChange.invoke(it)
                        viewModel.saveSound(it)
                    }
                )

                SwitchRow(
                    text = Res.string.show_alert,
                    checked = state.showAlert,
                    onCheckedChange = viewModel::saveAlert
                )

                SwitchRow(
                    text = Res.string.keep_screen_on,
                    checked = state.keepScreenOn,
                    onCheckedChange = viewModel::saveKeepScreenOn
                )

                if (hasFocusMode && state.purchases.pro != ProAccess.HIDDEN) {
                    FocusModeRow(
                        checked = state.focusMode && state.purchases.pro == ProAccess.OPEN,
                        unlocked = state.purchases.pro == ProAccess.OPEN,
                        granted = { viewModel.focusModeGranted },
                        openAccess = system::openFocusModeAccess,
                        onChange = { on ->
                            if (on && state.purchases.pro != ProAccess.OPEN) {
                                onOpenPro(ProFeature.DND)
                            } else {
                                viewModel.setFocusMode(on)
                            }
                        }
                    )
                }

                PermissionNotice(
                    text = Res.string.notifications_off,
                    granted = system::notificationsAllowed,
                    open = system::openNotificationSettings
                )

                PermissionNotice(
                    text = Res.string.exact_alarms_off,
                    granted = system::exactAlarmsAllowed,
                    open = system::openExactAlarmSettings
                )
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item {
            BackupCard(
                state = state.backup,
                onExport = viewModel::exportTo,
                onImport = viewModel::importFrom,
                onConfirmImport = viewModel::confirmImport,
                onCancelImport = viewModel::cancelImport,
                onMessageShown = viewModel::onBackupMessageShown
            )
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
        item { PositiveText() }
        item { Spacer(modifier = Modifier.height(192.dp)) }
    }
}

/**
 * La misma dirección que la de Play Console: `store/privacy/README.md`. Sin navegador no hay a dónde ir,
 * y no es motivo para cerrar la app.
 */
private const val PRIVACY_URL = "https://flowtime.baltajmn.dev/"

/**
 * No molestar mientras se trabaja (#43). Sin acceso a No molestar, primero se explica y después se
 * manda a los ajustes del sistema; si se quita más tarde, un aviso como el de las alarmas exactas.
 * Sin Pro ([unlocked] a false) solo se ofrece Pro: no se pide un permiso para algo que no se puede usar.
 */
@Composable
private fun FocusModeRow(
    checked: Boolean,
    unlocked: Boolean,
    granted: () -> Boolean,
    openAccess: () -> Unit,
    onChange: (Boolean) -> Unit
) {
    var explain by rememberSaveable { mutableStateOf(false) }
    SwitchRow(
        text = Res.string.focus_mode,
        checked = checked,
        onCheckedChange = { on ->
            onChange(on)
            if (on && unlocked && !granted()) explain = true
        }
    )
    if (checked) {
        PermissionNotice(text = Res.string.focus_mode_access_off, granted = granted, open = openAccess)
    }
    if (explain) {
        AlertDialog(
            onDismissRequest = { explain = false },
            title = { Text(text = stringResource(Res.string.focus_mode)) },
            text = { Text(text = stringResource(Res.string.focus_mode_access_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        explain = false
                        openAccess()
                    }
                ) { Text(text = stringResource(Res.string.turn_on)) }
            },
            dismissButton = {
                TextButton(onClick = { explain = false }) {
                    Text(text = stringResource(Res.string.notifications_later))
                }
            }
        )
    }
}

/**
 * Solo se ve si falta un permiso. Se vuelve a mirar al volver de los ajustes del sistema, y cuando
 * cambia un estado que lea [granted]: en el iPhone, el permiso llega un momento después.
 */
@Composable
fun PermissionNotice(text: StringResource, granted: () -> Boolean, open: () -> Unit) {
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val allowed by remember(lifecycle) { derivedStateOf(granted) }
    if (allowed) return
    ButtonRow(text = text, button = Res.string.turn_on, onClick = open)
}

/**
 * Una sección de Ajustes: el título a la izquierda, en el color del texto, y debajo lo suyo. El de
 * acento se queda para lo que se toca y lo que se mide.
 */
@Composable
fun SettingsCard(title: StringResource, content: @Composable ColumnScope.() -> Unit) {
    FlowCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = stringResource(title),
                modifier = Modifier.semantics { heading() },
                style = SmallTitle.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            content()
        }
    }
}

@Composable
fun ButtonRow(text: StringResource, button: StringResource, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(text),
            style = SubBody.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        TextButton(onClick = onClick) { Text(text = stringResource(button)) }
    }
}

/** Toda la fila cambia el interruptor, y TalkBack la lee como uno solo. */
@Composable
fun SwitchRow(
    text: StringResource,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            text = stringResource(text),
            style = SubBody.copy(color = MaterialTheme.colorScheme.onSurface)
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** El cierre de Ajustes: una frase, sin tarjeta. */
@Composable
fun PositiveText() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.remember),
            textAlign = TextAlign.Center,
            style = SubBody.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                MotivationalPhrases.entries[
                    (MotivationalPhrases.entries.toTypedArray().indices).random()
                ].resourceId
            ),
            textAlign = TextAlign.Center,
            style = SubBody.copy(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        )
    }
}

/** Ajustes › FlowTime Pro: lo que incluye, o las gracias a quien ya lo tiene. */
@Composable
fun ProCard(owned: Boolean, onOpen: () -> Unit) {
    SettingsCard(Res.string.pro_title) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                if (owned) Res.string.pro_settings_owned else Res.string.pro_settings_text
            ),
            style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        if (!owned) {
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onOpen) { Text(text = stringResource(Res.string.pro_see)) }
        }
    }
}

/** Las propinas, la insignia de quien ya ha dejado alguna y "Restaurar compras", siempre a la vista. */
@Composable
fun SupportCard(
    state: PurchasesUiState,
    onTip: () -> Unit,
    onRestore: () -> Unit,
    onMessageShown: () -> Unit
) {
    val showMessage = rememberShowMessage()
    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        val text = when (message) {
            RestoreMessage.RESTORED -> Res.string.restore_done
            RestoreMessage.NOTHING -> Res.string.restore_nothing
        }
        showMessage(getString(text))
        onMessageShown()
    }
    SettingsCard(Res.string.support_developer_title) {
        if (state.isSupporter) {
            Spacer(modifier = Modifier.height(8.dp))
            AssistChip(
                onClick = onTip,
                label = { Text(text = stringResource(Res.string.supporter_badge)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null
                    )
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                if (state.isSupporter) Res.string.supporter_thanks else Res.string.support_developer_description
            ),
            style = SubBody.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(onClick = onTip) { Text(text = stringResource(Res.string.support_developer)) }
        TextButton(onClick = onRestore, enabled = !state.restoring) {
            Text(text = stringResource(Res.string.restore_purchases))
        }
    }
}

/** Claro u oscuro, los colores del fondo de pantalla (desde Android 12), el tema y el icono de la app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceCard(
    appearance: Appearance,
    isSupporter: Boolean,
    onDarkMode: (DarkMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onTheme: (AppTheme) -> Unit,
    pro: ProAccess = ProAccess.HIDDEN,
    onLockedTheme: () -> Unit = {},
    icon: AppIcon = AppIcon.DEFAULT,
    onIcon: (AppIcon) -> Unit = {}
) {
    // Con los colores del fondo de pantalla, el tema no se usa: se ve, pero apagado.
    val themesEnabled = !(hasWallpaperColors && appearance.dynamicColor)

    SettingsCard(Res.string.appearance_title) {
        Spacer(modifier = Modifier.height(12.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            DarkMode.entries.forEachIndexed { index, mode ->
                SegmentedButton(
                    selected = appearance.darkMode == mode,
                    onClick = { onDarkMode(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index, DarkMode.entries.size),
                    colors = quietSegmentedColors()
                ) {
                    Text(text = stringResource(mode.label))
                }
            }
        }

        if (hasWallpaperColors) {
            Spacer(modifier = Modifier.height(4.dp))
            SwitchRow(
                text = Res.string.appearance_dynamic,
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
            // El tema Supporter es el regalo de las propinas (#58): solo lo ve quien lo tiene. Los de
            // Pro, solo con Pro a la venta. El que ya está puesto se ve siempre.
            items(
                AppTheme.entries.filter { theme ->
                    theme == appearance.theme ||
                        ((theme != AppTheme.Supporter || isSupporter) && (!theme.pro || pro != ProAccess.HIDDEN))
                }
            ) { theme ->
                val locked = theme.pro && pro == ProAccess.LOCKED && theme != appearance.theme
                ThemeSwatch(
                    theme = theme,
                    selected = theme == appearance.theme,
                    enabled = themesEnabled,
                    locked = locked,
                    onClick = { if (locked) onLockedTheme() else onTheme(theme) }
                )
            }
        }

        // Los iconos son de Pro: sin Pro a la venta, ni se ven.
        if (pro != ProAccess.HIDDEN) {
            Spacer(modifier = Modifier.height(12.dp))
            AppIconRow(icon = icon, pro = pro, onIcon = onIcon, onLocked = onLockedTheme)
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    selected: Boolean,
    enabled: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    val name = stringResource(theme.label)
    val description = if (locked) {
        stringResource(Res.string.cd_pro_theme, name)
    } else {
        stringResource(Res.string.cd_theme_color, name)
    }
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
        // Sobre el color de muestra, no sobre el tema: blanco o negro según lo claro que sea.
        val ink = if (theme.color.luminance() > 0.5f) Color.Black else Color.White
        if (selected) {
            Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = ink)
        } else if (locked) {
            Icon(
                painter = painterResource(Res.drawable.ic_lock_on),
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
