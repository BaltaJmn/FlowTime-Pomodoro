package com.baltajmn.flowtime.core.design.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.sound.PlayerState
import com.baltajmn.flowtime.core.design.sound.PlayerType
import com.baltajmn.flowtime.core.design.sound.PlayerType.BROWN
import com.baltajmn.flowtime.core.design.sound.PlayerType.PINK
import com.baltajmn.flowtime.core.design.sound.PlayerType.WHITE
import org.koin.compose.koinInject

private val SPACER = 10.dp

/** El botón de los sonidos de la pantalla de concentración: el ecualizador, quieto si no suena nada. */
@Composable
fun SoundButton(playing: Boolean, onClick: () -> Unit) {
    val description = stringResource(if (playing) R.string.cd_sounds_playing else R.string.cd_sounds_stopped)
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        LottieImage(
            modifier = Modifier.size(30.dp),
            animation = R.raw.equalizer,
            // Animar siempre redibuja la pantalla sin parar y gasta batería.
            playing = playing,
            tintColor = if (playing) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            }
        )
    }
}

/** Los sonidos ambientales, en una hoja que se abre desde la pantalla de concentración. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundSheet(onDismiss: () -> Unit, ambience: Ambience = koinInject()) {
    val sound by ambience.state.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.sounds_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            ExpandedContent(
                items = sound.soundMap,
                onPlayClicked = { type, playing -> ambience.play(type = type, playing = playing) },
                onVolumeChanged = { type, volume -> ambience.setVolume(type = type, volume = volume) }
            )
        }
    }
}

@Composable
fun ExpandedContent(
    items: Map<PlayerType, PlayerState>,
    onPlayClicked: (PlayerType, Boolean) -> Unit,
    onVolumeChanged: (PlayerType, Float) -> Unit
) {
    val mixerDescription = stringResource(R.string.cd_sound_mixer)

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .semantics {
                contentDescription = mixerDescription
            }
    ) {
        PlayerType.entries.forEach { playerType ->
            val playerState = items[playerType]
            if (playerState != null) {
                SliderItem(
                    type = playerType,
                    playerState = playerState,
                    onPlayClicked = onPlayClicked,
                    onVolumeChanged = onVolumeChanged
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
    onVolumeChanged: (PlayerType, Float) -> Unit
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
            contentDescription = soundName,
            tint = iconTint ?: MaterialTheme.colorScheme.secondary
        )

        Slider(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .weight(1f),
            value = volume,
            onValueChange = { newVolume ->
                onVolumeChanged(type, newVolume)
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            ),
            valueRange = 0f..1f
        )

        IconButton(
            onClick = {
                onPlayClicked(type, !isPlaying)
            }
        ) {
            Icon(
                painter = painterResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
                ),
                tint = MaterialTheme.colorScheme.secondary,
                contentDescription = stringResource(
                    if (isPlaying) R.string.cd_pause_sound else R.string.cd_play_sound,
                    soundName
                )
            )
        }
    }
}
