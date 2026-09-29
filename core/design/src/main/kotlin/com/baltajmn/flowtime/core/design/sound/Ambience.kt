package com.baltajmn.flowtime.core.design.sound

import android.content.Context
import android.content.Intent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Qué sonidos ambientales suenan y a qué volumen. Vive fuera de las pantallas porque sigue sonando
 * con la app en segundo plano: el panel y [AmbientService] tocan el mismo estado.
 */
class Ambience(
    private val context: Context,
    private val dataProvider: DataProvider,
    private val mixer: AmbientMixer
) {
    private val _state = MutableStateFlow(
        SoundState(
            PlayerType.entries.associateWith {
                PlayerState(volume = dataProvider.getFloat(it.name, DEFAULT_VOLUME))
            }
        )
    )
    val state: StateFlow<SoundState> = _state.asStateFlow()

    /** Solo desde la app a la vista: arrancar el servicio desde segundo plano no está permitido. */
    fun play(type: PlayerType, playing: Boolean) {
        edit { it.copy(soundMap = it.soundMap.with(type) { copy(isPlaying = playing) }, paused = emptySet()) }
        if (playing) ContextCompat.startForegroundService(context, Intent(context, AmbientService::class.java))
    }

    fun setVolume(type: PlayerType, volume: Float) {
        dataProvider.setFloat(type.name, volume)
        edit { it.copy(soundMap = it.soundMap.with(type) { copy(volume = volume) }) }
    }

    fun pause() = edit { s ->
        if (s.playing.isEmpty()) s else SoundState(s.soundMap.stopped(), paused = s.playing)
    }

    fun resume() = edit { s ->
        SoundState(s.soundMap.mapValues { (type, p) -> p.copy(isPlaying = p.isPlaying || type in s.paused) })
    }

    fun stop() = edit { SoundState(it.soundMap.stopped()) }

    private fun edit(change: (SoundState) -> SoundState) {
        _state.update(change)
        _state.value.soundMap.forEach { (type, p) -> mixer.setVolume(type, if (p.isPlaying) p.volume else 0f) }
    }

    private fun Map<PlayerType, PlayerState>.with(type: PlayerType, change: PlayerState.() -> PlayerState) =
        this + (type to getValue(type).change())

    private fun Map<PlayerType, PlayerState>.stopped() = mapValues { it.value.copy(isPlaying = false) }

    private companion object {
        // Antes era 0: al darle a reproducir un sonido nuevo no se oia nada.
        const val DEFAULT_VOLUME = 0.5f
    }
}

data class SoundState(
    val soundMap: Map<PlayerType, PlayerState> = PlayerType.entries.associateWith { PlayerState() },
    /** Lo que sonaba antes de una pausa (una llamada, los auriculares, la notificación), para reanudarlo. */
    val paused: Set<PlayerType> = emptySet()
) {
    val playing: Set<PlayerType> get() = soundMap.filterValues { it.isPlaying }.keys
}

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
