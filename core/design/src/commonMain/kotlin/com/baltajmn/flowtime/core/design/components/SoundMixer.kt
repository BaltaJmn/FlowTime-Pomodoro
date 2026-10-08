package com.baltajmn.flowtime.core.design.components

import com.baltajmn.flowtime.core.design.theme.SheetTitle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.baltajmn.flowtime.core.design.sound.SleepTimer
import com.baltajmn.flowtime.core.design.sound.SoundMix
import com.baltajmn.flowtime.core.design.sound.SoundMixes
import com.baltajmn.flowtime.core.design.sound.SoundState
import kotlinx.coroutines.delay
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Clock
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.sound.PlayerState
import com.baltajmn.flowtime.core.design.sound.PlayerType
import com.baltajmn.flowtime.core.design.sound.PlayerType.BROWN
import com.baltajmn.flowtime.core.design.sound.PlayerType.PINK
import com.baltajmn.flowtime.core.design.sound.PlayerType.WHITE

private val SPACER = 10.dp

/** El botón de los sonidos de la pantalla de concentración: el ecualizador, quieto si no suena nada. */
@Composable
fun SoundButton(playing: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (playing) Res.string.cd_sounds_playing else Res.string.cd_sounds_stopped)
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        if (playing) {
            // Solo se anima mientras suena: animar siempre redibuja la pantalla y gasta batería.
            LottieImage(
                modifier = Modifier.size(30.dp),
                animation = LottieAnimation.EQUALIZER,
                playing = true,
                tintColor = MaterialTheme.colorScheme.primary
            )
        } else {
            // Parado, una nota: el ecualizador quieto no se leía como sonidos.
            Icon(
                painter = painterResource(Res.drawable.ic_music),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Los sonidos ambientales, en una hoja que se abre desde la pantalla de concentración: las mezclas
 * guardadas, el temporizador de apagado y un deslizador por sonido. [mixLimit] es cuántas mezclas se
 * pueden guardar sin Pro, o null sin límite; al llegar, [onSeePro] abre la pantalla de Pro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSheet(
    onDismiss: () -> Unit,
    mixLimit: Int? = null,
    onSeeProMixes: () -> Unit = {},
    showProSounds: Boolean = false,
    proSoundsLocked: Boolean = false,
    onSeeProSounds: () -> Unit = {},
    ambience: Ambience,
    mixes: SoundMixes
) {
    val sound by ambience.state.collectAsState()
    val sleep by ambience.sleep.collectAsState()
    val all by mixes.all.collectAsState()
    var dialog by remember { mutableStateOf<MixDialog?>(null) }

    // Un sonido de Pro sin Pro se escucha 10 segundos; al cerrar la hoja, se corta.
    var preview by remember { mutableStateOf<PlayerType?>(null) }
    LaunchedEffect(preview) {
        val type = preview ?: return@LaunchedEffect
        delay(PREVIEW_MILLIS)
        ambience.play(type, false)
        preview = null
    }
    DisposableEffect(Unit) { onDispose { preview?.let { ambience.play(it, false) } } }
    val locked = { type: PlayerType -> type.pro && proSoundsLocked }

    // Entera desde el principio: a medias, los sonidos quedaban debajo del borde.
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        SoundSheetBody(
            sound = sound,
            sleep = sleep,
            mixes = all,
            onSleep = ambience::setSleep,
            onMix = ambience::load,
            onEditMix = { dialog = MixDialog.Edit(it) },
            onSaveMix = {
                val full = mixLimit != null && mixes.saved >= mixLimit
                dialog = if (full) MixDialog.Limit else MixDialog.Save
            },
            onPlay = { type, playing ->
                ambience.play(type = type, playing = playing)
                if (locked(type)) preview = type.takeIf { playing }
            },
            onVolume = { type, volume -> ambience.setVolume(type = type, volume = volume) },
            showProSounds = showProSounds,
            proSoundsLocked = proSoundsLocked,
            onLocked = onSeeProSounds
        )
    }

    when (val open = dialog) {
        MixDialog.Save -> NameDialog(
            initial = "",
            onConfirm = { name ->
                mixes.save(name, ambience.current().filterKeys { !locked(it) })
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        is MixDialog.Edit -> EditMixDialog(
            mix = open.mix,
            onRename = { name ->
                mixes.rename(open.mix.id, name)
                dialog = null
            },
            onDelete = {
                mixes.delete(open.mix)
                dialog = null
            },
            onDismiss = { dialog = null }
        )
        MixDialog.Limit -> AlertDialog(
            onDismissRequest = { dialog = null },
            text = { Text(text = stringResource(Res.string.mix_limit, mixLimit ?: 0)) },
            confirmButton = {
                TextButton(onClick = { dialog = null }) { Text(text = stringResource(Res.string.dialog_confirm)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        dialog = null
                        onSeeProMixes()
                    }
                ) { Text(text = stringResource(Res.string.pro_see)) }
            }
        )
        null -> Unit
    }
}

/** Lo de dentro de la hoja de sonidos, sin la hoja: lo pinta también la captura de la ficha (#54). */
@Composable
fun SoundSheetBody(
    sound: SoundState,
    sleep: SleepTimer?,
    mixes: List<SoundMix>,
    onSleep: (SleepTimer?) -> Unit,
    onMix: (SoundMix) -> Unit,
    onEditMix: (SoundMix) -> Unit,
    onSaveMix: () -> Unit,
    onPlay: (PlayerType, Boolean) -> Unit,
    onVolume: (PlayerType, Float) -> Unit,
    showProSounds: Boolean = false,
    proSoundsLocked: Boolean = false,
    onLocked: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.sounds_title),
                style = SheetTitle,
                modifier = Modifier.weight(1f)
            )
            SleepButton(sleep = sleep, onSleep = onSleep)
        }
        sleep?.let { timer ->
            Text(
                text = when (timer) {
                    is SleepTimer.At -> stringResource(
                        Res.string.sleep_at,
                        timeOfDay(timer.millis)
                    )
                    SleepTimer.SessionEnd -> stringResource(Res.string.sleep_at_session_end)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            mixes.forEach { mix ->
                MixChip(
                    text = mix.label?.let { stringResource(it) } ?: mix.name,
                    onClick = { onMix(mix) },
                    onLongClick = { onEditMix(mix) }
                )
            }
            if (sound.playing.isNotEmpty()) {
                MixChip(
                    text = stringResource(Res.string.mix_save),
                    icon = true,
                    onClick = onSaveMix
                )
            }
        }
        ExpandedContent(
            items = sound.soundMap,
            onPlayClicked = onPlay,
            onVolumeChanged = onVolume,
            showPro = showProSounds,
            proLocked = proSoundsLocked,
            onLocked = { onLocked() }
        )
    }
}

private sealed interface MixDialog {
    data object Save : MixDialog

    data object Limit : MixDialog

    data class Edit(val mix: SoundMix) : MixDialog
}

/** Apagar a los 15, 30, 45 o 60 minutos, o al terminar la sesión. */
@Composable
private fun SleepButton(sleep: SleepTimer?, onSleep: (SleepTimer?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(
                painter = painterResource(Res.drawable.ic_timer),
                contentDescription = stringResource(Res.string.sleep_title),
                tint = if (sleep != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val choose = { timer: SleepTimer? ->
                onSleep(timer)
                open = false
            }
            DropdownMenuItem(text = { Text(text = stringResource(Res.string.sleep_off)) }, onClick = { choose(null) })
            SLEEP_MINUTES.forEach { minutes ->
                DropdownMenuItem(
                    text = { Text(text = stringResource(Res.string.sleep_minutes, minutes)) },
                    onClick = { choose(SleepTimer.At(Clock.System.now().toEpochMilliseconds() + minutes * 60_000L)) }
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(Res.string.sleep_session_end)) },
                onClick = { choose(SleepTimer.SessionEnd) }
            )
        }
    }
}

private val SLEEP_MINUTES = listOf(15, 30, 45, 60)
private const val PREVIEW_MILLIS = 10_000L

/** Un chip que también se puede mantener pulsado: los de Material no lo permiten. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MixChip(text: String, onClick: () -> Unit, onLongClick: (() -> Unit)? = null, icon: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, role = Role.Button)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon) Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(text = text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun NameDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(Res.string.mix_save)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(MAX_NAME) },
                label = { Text(text = stringResource(Res.string.mix_name)) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(text = stringResource(Res.string.dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(Res.string.dialog_cancel)) }
        }
    )
}

/** Mantener pulsada una mezcla: cambiarle el nombre o borrarla; las de ejemplo, solo ocultarlas. */
@Composable
private fun EditMixDialog(mix: SoundMix, onRename: (String) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(mix.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = mix.label?.let { stringResource(it) } ?: mix.name) },
        text = if (mix.builtIn) {
            null
        } else {
            {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MAX_NAME) },
                    label = { Text(text = stringResource(Res.string.mix_name)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            if (!mix.builtIn) {
                TextButton(onClick = { onRename(name) }, enabled = name.isNotBlank()) {
                    Text(text = stringResource(Res.string.dialog_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text(text = stringResource(if (mix.builtIn) Res.string.mix_hide else Res.string.mix_delete))
            }
        }
    )
}

private const val MAX_NAME = 30

@Composable
fun ExpandedContent(
    items: Map<PlayerType, PlayerState>,
    onPlayClicked: (PlayerType, Boolean) -> Unit,
    onVolumeChanged: (PlayerType, Float) -> Unit,
    /** Los sonidos de Pro: sin Pro a la venta no salen; sin comprar, con un candado. */
    showPro: Boolean = false,
    proLocked: Boolean = false,
    onLocked: (PlayerType) -> Unit = {}
) {
    val mixerDescription = stringResource(Res.string.cd_sound_mixer)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = mixerDescription
            }
    ) {
        PlayerType.entries.filter { !it.pro || showPro }.forEach { playerType ->
            val playerState = items[playerType]
            if (playerState != null) {
                SliderItem(
                    type = playerType,
                    playerState = playerState,
                    onPlayClicked = onPlayClicked,
                    onVolumeChanged = onVolumeChanged,
                    onLocked = if (playerType.pro && proLocked) ({ onLocked(playerType) }) else null
                )
            }
        }
    }
}

@Composable
fun SliderItem(
    type: PlayerType,
    playerState: PlayerState,
    onPlayClicked: (PlayerType, Boolean) -> Unit,
    onVolumeChanged: (PlayerType, Float) -> Unit,
    /** Con candado: un sonido de Pro sin Pro, que abre la pantalla de Pro. */
    onLocked: (() -> Unit)? = null
) {
    val isPlaying = playerState.isPlaying
    val volume = playerState.volume

    val iconTint = remember(type) {
        when (type) {
            BROWN, PINK, WHITE -> Color.Unspecified
            else -> null
        }
    }

    val soundName = stringResource(type.label)
    // Lo que suena va en el acento; lo parado se queda en gris.
    val tone = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(SPACER)
    ) {
        Icon(
            // Los ruidos son círculos de su color: con borde, el blanco se ve también en el tema claro.
            modifier = if (iconTint != null) {
                Modifier.border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
            } else {
                Modifier
            },
            painter = painterResource(type.icon),
            // El nombre ya se lee en el deslizador.
            contentDescription = null,
            tint = iconTint ?: tone
        )

        // Pista fina y tirador corto: once barras gruesas una encima de otra pesaban demasiado.
        val colors = SliderDefaults.colors(thumbColor = tone, activeTrackColor = tone)
        val interaction = remember { MutableInteractionSource() }
        // Con nombre: TalkBack decía solo "control deslizante, 40 %".
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
        // A la vista: los iconos solos no decían qué sonaba.
        Text(
            text = soundName,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = if (isPlaying) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {}
        )
        Slider(
            modifier = Modifier
                .semantics { contentDescription = soundName },
            value = volume,
            onValueChange = { newVolume ->
                onVolumeChanged(type, newVolume)
            },
            colors = colors,
            interactionSource = interaction,
            thumb = {
                SliderDefaults.Thumb(interactionSource = interaction, colors = colors, thumbSize = DpSize(4.dp, 24.dp))
            },
            track = {
                SliderDefaults.Track(
                    sliderState = it,
                    modifier = Modifier.height(6.dp),
                    colors = colors,
                    drawStopIndicator = null,
                    thumbTrackGapSize = 4.dp
                )
            },
            valueRange = 0f..1f
        )
        }

        IconButton(
            onClick = {
                onPlayClicked(type, !isPlaying)
            }
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) Res.drawable.ic_pause else Res.drawable.ic_play
                ),
                tint = tone,
                contentDescription = stringResource(
                    if (isPlaying) Res.string.cd_pause_sound else Res.string.cd_play_sound,
                    soundName
                )
            )
        }

        onLocked?.let {
            IconButton(onClick = it) {
                Icon(
                    painter = painterResource(Res.drawable.ic_lock_on),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = stringResource(Res.string.cd_pro_sound, soundName)
                )
            }
        }
    }
}
