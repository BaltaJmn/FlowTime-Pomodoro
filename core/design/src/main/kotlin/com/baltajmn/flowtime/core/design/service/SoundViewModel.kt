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

    fun controlSounds(playerType: PlayerType, playing: Boolean) {
        when (playerType) {
            RAIN -> {
                rain?.let { startStop(rain, playing) }
                    ?: run {
                        rain = getSoundPlayer(RAIN)
                        rain?.start()
                    }
            }

            FIRE -> {
                fire?.let { startStop(fire, playing) }
                    ?: run {
                        fire = getSoundPlayer(FIRE)
                        fire?.start()
                    }
            }

            WAVE -> {
                wave?.let { startStop(wave, playing) }
                    ?: run {
                        wave = getSoundPlayer(WAVE)
                        wave?.start()
                    }
            }

            THUNDER -> {
                thunder?.let { startStop(thunder, playing) }
                    ?: run {
                        thunder = getSoundPlayer(THUNDER)
                        thunder?.start()
                    }
            }

            BIRDS -> {
                birds?.let { startStop(birds, playing) }
                    ?: run {
                        birds = getSoundPlayer(BIRDS)
                        birds?.start()
                    }
            }

            HEAT -> {
                heat?.let { startStop(heat, playing) }
                    ?: run {
                        heat = getSoundPlayer(HEAT)
                        heat?.start()
                    }
            }

            COFFEE_HOUSE -> {
                coffeeHouse?.let { startStop(coffeeHouse, playing) }
                    ?: run {
                        coffeeHouse = getSoundPlayer(COFFEE_HOUSE)
                        coffeeHouse?.start()
                    }
            }

            MEDITATION -> {
                meditation?.let { startStop(meditation, playing) }
                    ?: run {
                        meditation = getSoundPlayer(MEDITATION)
                        meditation?.start()
                    }
            }

            WIND -> {
                wind?.let { startStop(wind, playing) }
                    ?: run {
                        wind = getSoundPlayer(WIND)
                        wind?.start()
                    }
            }

            BROWN -> {
                brown?.let { startStop(brown, playing) }
                    ?: run {
                        brown = getSoundPlayer(BROWN)
                        brown?.start()
                    }
            }

            PINK -> {
                pink?.let { startStop(pink, playing) }
                    ?: run {
                        pink = getSoundPlayer(PINK)
                        pink?.start()
                    }
            }

            WHITE -> {
                white?.let { startStop(white, playing) }
                    ?: run {
                        white = getSoundPlayer(WHITE)
                        white?.start()
                    }
            }
        }
    }

    private fun startStop(player: MediaPlayer?, playing: Boolean) {
        player?.let {
            if (playing) {
                it.start()
            } else {
                it.pause()
            }
        }
    }

    fun setVolume(player: PlayerType, volume: Float) {
        _uiState.update {
            it.copy(
                soundMap = it.soundMap.apply {
                    this[player] = volume
                }
            )
        }

        val selectedVolume = _uiState.value.soundMap[player] ?: 0f

        when (player) {
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
        val selectedVolume = _uiState.value.soundMap[type] ?: 0f

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
    val soundMap: MutableMap<PlayerType, Float> = mutableMapOf(
        RAIN to 0f,
        FIRE to 0f,
        WAVE to 0f,
        THUNDER to 0f,
        BIRDS to 0f,
        HEAT to 0f,
        COFFEE_HOUSE to 0f,
        MEDITATION to 0f,
        WIND to 0f,
        BROWN to 0f,
        PINK to 0f,
        WHITE to 0f
    )
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