package com.baltajmn.flowtime.core.design.service

import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaPlayer
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.service.PlayerType.BIRDS
import com.baltajmn.flowtime.core.design.service.PlayerType.BROWN
import com.baltajmn.flowtime.core.design.service.PlayerType.COFFEE_HOUSE
import com.baltajmn.flowtime.core.design.service.PlayerType.FIRE
import com.baltajmn.flowtime.core.design.service.PlayerType.HEAT
import com.baltajmn.flowtime.core.design.service.PlayerType.MEDITATION
import com.baltajmn.flowtime.core.design.service.PlayerType.PINK
import com.baltajmn.flowtime.core.design.service.PlayerType.RAIN
import com.baltajmn.flowtime.core.design.service.PlayerType.THUNDER
import com.baltajmn.flowtime.core.design.service.PlayerType.WAVE
import com.baltajmn.flowtime.core.design.service.PlayerType.WHITE
import com.baltajmn.flowtime.core.design.service.PlayerType.WIND
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class SoundViewModel : ViewModel() {

    private val players: MutableMap<PlayerType, MediaPlayer> = mutableMapOf()

    private val _uiState = MutableStateFlow(SoundState())
    val uiState: StateFlow<SoundState> = _uiState

    fun getItems(): MutableMap<PlayerType, PlayerState> = _uiState.value.soundMap

    fun muteAll() {
        PlayerType.entries.forEach { type ->
            controlSounds(type, false)
        }
    }

    fun controlSounds(playerType: PlayerType, playing: Boolean) {
        val player = players.getOrPut(playerType) {
            getSoundPlayer(playerType)
        }

        if (playing) {
            player.start()
        } else {
            player.pause()
        }

        updateIsPlaying(playerType, playing)
    }

    fun setVolume(type: PlayerType, volume: Float) {
        _uiState.update { state ->
            state.copy(
                soundMap = state.soundMap.apply {
                    this[type]?.volume = volume
                }
            )
        }
        players[type]?.setVolume(volume, volume)
    }

    private fun updateIsPlaying(playerType: PlayerType, playing: Boolean) {
        _uiState.update { current ->
            current.copy(
                soundMap = current.soundMap.apply {
                    this[playerType]?.isPlaying = playing
                }
            )
        }
    }

    private fun getSoundPlayer(type: PlayerType) = MediaPlayer().apply {
        val selectedVolume = _uiState.value.soundMap[type]?.volume ?: 0f

        isLooping = true
        setAudioStreamType(STREAM_MUSIC)
        setDataSource("https://mynoise.world/NoisesOnline/Audio/${type.sound}.ogg")
        setOnPreparedListener { mp ->
            // Inicia el reproductor una vez preparado
            mp.start()
        }
        setVolume(selectedVolume, selectedVolume)
        prepareAsync()
    }
}

data class SoundState(
    val soundMap: MutableMap<PlayerType, PlayerState> = mutableMapOf(
        RAIN to PlayerState(),
        FIRE to PlayerState(),
        WAVE to PlayerState(),
        THUNDER to PlayerState(),
        BIRDS to PlayerState(),
        HEAT to PlayerState(),
        COFFEE_HOUSE to PlayerState(),
        MEDITATION to PlayerState(),
        WIND to PlayerState(),
        BROWN to PlayerState(),
        PINK to PlayerState(),
        WHITE to PlayerState()
    )
)

data class PlayerState(
    var volume: Float = 0f,
    var isPlaying: Boolean = false
)

enum class PlayerType(@DrawableRes val icon: Int, val sound: String) {
    RAIN(R.drawable.ic_rain, "cb"),
    FIRE(R.drawable.ic_fire, "ea"),
    WAVE(R.drawable.ic_wave, "be"),
    THUNDER(R.drawable.ic_thunder, "cd"),
    BIRDS(R.drawable.ic_bird, "da"),
    HEAT(R.drawable.ic_heat, "dd"),
    COFFEE_HOUSE(R.drawable.ic_coffee, "fa"),
    MEDITATION(R.drawable.ic_meditation, "ga"),
    WIND(R.drawable.ic_wind, "gb"),
    BROWN(R.drawable.ic_brown, "ia"),
    PINK(R.drawable.ic_pink, "ib"),
    WHITE(R.drawable.ic_white, "ic")
}
