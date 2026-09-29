package com.baltajmn.flowtime.core.design.service

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.sound.AmbientMixer
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SoundViewModel(
    private val dataProvider: DataProvider,
    private val mixer: AmbientMixer
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SoundState(
            PlayerType.entries.associateWith {
                PlayerState(volume = dataProvider.getFloat(it.name, DEFAULT_VOLUME))
            }
        )
    )
    val uiState: StateFlow<SoundState> = _uiState.asStateFlow()

    fun controlSounds(playerType: PlayerType, playing: Boolean) =
        update(playerType) { it.copy(isPlaying = playing) }

    fun setVolume(type: PlayerType, volume: Float) {
        dataProvider.setFloat(type.name, volume)
        update(type) { it.copy(volume = volume) }
    }

    /** Silencia sin olvidar que sonaba, para que [resumePlayingPlayers] lo recupere. */
    fun pauseAllPlayers() = PlayerType.entries.forEach { mixer.setVolume(it, 0f) }

    fun resumePlayingPlayers() = _uiState.value.soundMap.forEach(::apply)

    private fun update(type: PlayerType, change: (PlayerState) -> PlayerState) {
        _uiState.update { state ->
            state.copy(soundMap = state.soundMap + (type to change(state.soundMap.getValue(type))))
        }
        apply(type, _uiState.value.soundMap.getValue(type))
    }

    private fun apply(type: PlayerType, state: PlayerState) =
        mixer.setVolume(type, if (state.isPlaying) state.volume else 0f)

    override fun onCleared() = pauseAllPlayers()

    private companion object {
        // Antes era 0: al darle a reproducir un sonido nuevo no se oia nada.
        const val DEFAULT_VOLUME = 0.5f
    }
}

data class SoundState(
    val soundMap: Map<PlayerType, PlayerState> = PlayerType.entries.associateWith { PlayerState() }
)

data class PlayerState(
    val volume: Float = 0f,
    val isPlaying: Boolean = false
)

enum class PlayerType(@DrawableRes val icon: Int, @StringRes val label: Int) {
    RAIN(R.drawable.ic_rain, R.string.sound_rain),
    FIRE(R.drawable.ic_fire, R.string.sound_fire),
    WAVE(R.drawable.ic_wave, R.string.sound_wave),
    THUNDER(R.drawable.ic_thunder, R.string.sound_thunder),
    BIRDS(R.drawable.ic_bird, R.string.sound_birds),
    HEAT(R.drawable.ic_heat, R.string.sound_heat),
    COFFEE_HOUSE(R.drawable.ic_coffee, R.string.sound_coffee_house),
    MEDITATION(R.drawable.ic_meditation, R.string.sound_meditation),
    WIND(R.drawable.ic_wind, R.string.sound_wind),
    BROWN(R.drawable.ic_brown, R.string.sound_brown),
    PINK(R.drawable.ic_pink, R.string.sound_pink),
    WHITE(R.drawable.ic_white, R.string.sound_white)
}
