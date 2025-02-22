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
    private var rain: MediaPlayer? = null
    private var fire: MediaPlayer? = null
    private var wave: MediaPlayer? = null
    private var thunder: MediaPlayer? = null
    private var birds: MediaPlayer? = null
    private var heat: MediaPlayer? = null
    private var coffeeHouse: MediaPlayer? = null
    private var meditation: MediaPlayer? = null
    private var wind: MediaPlayer? = null
    private var brown: MediaPlayer? = null
    private var pink: MediaPlayer? = null
    private var white: MediaPlayer? = null

    private val _uiState = MutableStateFlow(SoundState())
    val uiState: StateFlow<SoundState> = _uiState

    fun getItems(): MutableMap<PlayerType, PlayerState> {
        return _uiState.value.soundMap
    }

    fun controlSounds(playerType: PlayerType, playing: Boolean) {
        when (playerType) {
            RAIN -> {
                rain?.let { startStop(playerType, rain, playing) }
                    ?: run {
                        rain = getSoundPlayer(RAIN)
                        rain?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            FIRE -> {
                fire?.let { startStop(playerType, fire, playing) }
                    ?: run {
                        fire = getSoundPlayer(FIRE)
                        fire?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            WAVE -> {
                wave?.let { startStop(playerType, wave, playing) }
                    ?: run {
                        wave = getSoundPlayer(WAVE)
                        wave?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            THUNDER -> {
                thunder?.let { startStop(playerType, thunder, playing) }
                    ?: run {
                        thunder = getSoundPlayer(THUNDER)
                        thunder?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            BIRDS -> {
                birds?.let { startStop(playerType, birds, playing) }
                    ?: run {
                        birds = getSoundPlayer(BIRDS)
                        birds?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            HEAT -> {
                heat?.let { startStop(playerType, heat, playing) }
                    ?: run {
                        heat = getSoundPlayer(HEAT)
                        heat?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            COFFEE_HOUSE -> {
                coffeeHouse?.let { startStop(playerType, coffeeHouse, playing) }
                    ?: run {
                        coffeeHouse = getSoundPlayer(COFFEE_HOUSE)
                        coffeeHouse?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            MEDITATION -> {
                meditation?.let { startStop(playerType, meditation, playing) }
                    ?: run {
                        meditation = getSoundPlayer(MEDITATION)
                        meditation?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            WIND -> {
                wind?.let { startStop(playerType, wind, playing) }
                    ?: run {
                        wind = getSoundPlayer(WIND)
                        wind?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            BROWN -> {
                brown?.let { startStop(playerType, brown, playing) }
                    ?: run {
                        brown = getSoundPlayer(BROWN)
                        brown?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            PINK -> {
                pink?.let { startStop(playerType, pink, playing) }
                    ?: run {
                        pink = getSoundPlayer(PINK)
                        pink?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }

            WHITE -> {
                white?.let { startStop(playerType, white, playing) }
                    ?: run {
                        white = getSoundPlayer(WHITE)
                        white?.start()

                        updateIsPlaying(playerType, playing)
                    }
            }
        }
    }

    private fun updateIsPlaying(playerType: PlayerType, playing: Boolean) {
        _uiState.update {
            it.copy(
                soundMap = it.soundMap.apply {
                    this[playerType]?.isPlaying = playing
                }
            )
        }
    }

    private fun startStop(playerType: PlayerType, mediaPlayer: MediaPlayer?, playing: Boolean) {
        mediaPlayer?.let { player ->
            if (playing) {
                player.start()
            } else {
                player.pause()
            }

            updateIsPlaying(playerType, playing)
        }
    }

    fun setVolume(type: PlayerType, volume: Float) {
        _uiState.update {
            it.copy(
                soundMap = it.soundMap.apply {
                    this[type]?.volume = volume
                }
            )
        }

        val selectedVolume = _uiState.value.soundMap[type]?.volume ?: 0f

        when (type) {
            RAIN -> rain?.setVolume(selectedVolume, selectedVolume)
            FIRE -> fire?.setVolume(selectedVolume, selectedVolume)
            WAVE -> wave?.setVolume(selectedVolume, selectedVolume)
            THUNDER -> thunder?.setVolume(selectedVolume, selectedVolume)
            BIRDS -> birds?.setVolume(selectedVolume, selectedVolume)
            HEAT -> heat?.setVolume(selectedVolume, selectedVolume)
            COFFEE_HOUSE -> coffeeHouse?.setVolume(selectedVolume, selectedVolume)
            MEDITATION -> meditation?.setVolume(selectedVolume, selectedVolume)
            WIND -> wind?.setVolume(selectedVolume, selectedVolume)
            BROWN -> brown?.setVolume(selectedVolume, selectedVolume)
            PINK -> pink?.setVolume(selectedVolume, selectedVolume)
            WHITE -> white?.setVolume(selectedVolume, selectedVolume)
        }
    }

    private fun getSoundPlayer(type: PlayerType) = MediaPlayer().apply {
        val selectedVolume = _uiState.value.soundMap[type]?.volume ?: 0f

        isLooping = true

        setAudioStreamType(STREAM_MUSIC)
        setDataSource("https://mynoise.world/NoisesOnline/Audio/${type.sound}.ogg")
        setOnPreparedListener {
            start()
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