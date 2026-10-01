package com.baltajmn.flowtime.core.design.sound

import com.baltajmn.flowtime.core.design.resources.*
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.DrawableResource

/**
 * Qué sonidos ambientales suenan y a qué volumen. Vive fuera de las pantallas porque sigue sonando
 * con la app en segundo plano: el panel y el servicio de cada plataforma tocan el mismo estado.
 * [keepPlaying] mantiene el sonido con la app en segundo plano (en Android, el servicio).
 */
class Ambience(
    private val dataProvider: DataProvider,
    private val mixer: AmbientMixer,
    private val keepPlaying: () -> Unit
) {
    private val _state = MutableStateFlow(
        SoundState(
            PlayerType.entries.associateWith {
                PlayerState(volume = dataProvider.getFloat(it.name, DEFAULT_VOLUME))
            }
        )
    )
    val state: StateFlow<SoundState> = _state.asStateFlow()

    private val _sleep = MutableStateFlow<SleepTimer?>(null)

    /** Cuándo se apagan solos los sonidos (#44); null, nunca. Lo cumple el servicio de cada plataforma. */
    val sleep: StateFlow<SleepTimer?> = _sleep.asStateFlow()

    /** Solo desde la app a la vista: arrancar el servicio desde segundo plano no está permitido. */
    fun play(type: PlayerType, playing: Boolean) {
        edit { it.copy(soundMap = it.soundMap.with(type) { copy(isPlaying = playing) }, paused = emptySet()) }
        if (playing) keepPlaying()
    }

    /** Lo que suena ahora, con su volumen: lo que se guarda como mezcla. */
    fun current(): Map<PlayerType, Float> = _state.value.soundMap.filterValues { it.isPlaying }.mapValues { it.value.volume }

    /** Suenan los sonidos de la mezcla, con sus volúmenes, y se paran los demás. */
    fun load(mix: SoundMix) {
        mix.volumes.forEach { (type, volume) -> dataProvider.setFloat(type.name, volume) }
        edit { s ->
            SoundState(
                s.soundMap.mapValues { (type, p) ->
                    mix.volumes[type]?.let { PlayerState(volume = it, isPlaying = true) } ?: p.copy(isPlaying = false)
                }
            )
        }
        if (mix.volumes.isNotEmpty()) keepPlaying()
    }

    fun setSleep(timer: SleepTimer?) {
        _sleep.value = timer
    }

    /** Baja el volumen de toda la mezcla en 5 segundos y para. */
    suspend fun fadeOutAndStop() {
        for (step in FADE_STEPS - 1 downTo 0) {
            mixer.master = step / FADE_STEPS.toFloat()
            delay(FADE_MILLIS / FADE_STEPS)
        }
        stop()
        mixer.master = 1f
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

    fun stop() {
        edit { SoundState(it.soundMap.stopped()) }
        _sleep.value = null
    }

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
        const val FADE_STEPS = 50
        const val FADE_MILLIS = 5_000L
    }
}

/** El temporizador de apagado: a una hora, o al terminar la sesión. */
sealed interface SleepTimer {
    data class At(val millis: Long) : SleepTimer

    data object SessionEnd : SleepTimer
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

/** Los sonidos, en el orden del panel. Los de [pro] se oyen 10 segundos sin Pro, y sin Pro a la venta no salen. */
enum class PlayerType(val icon: DrawableResource, val label: StringResource, val pro: Boolean = false) {
    RAIN(Res.drawable.ic_rain, Res.string.sound_rain),
    FIRE(Res.drawable.ic_fire, Res.string.sound_fire),
    WAVE(Res.drawable.ic_wave, Res.string.sound_wave),
    THUNDER(Res.drawable.ic_thunder, Res.string.sound_thunder),
    BIRDS(Res.drawable.ic_bird, Res.string.sound_birds),
    HEAT(Res.drawable.ic_heat, Res.string.sound_heat),
    COFFEE_HOUSE(Res.drawable.ic_coffee, Res.string.sound_coffee_house),
    MEDITATION(Res.drawable.ic_meditation, Res.string.sound_meditation),
    WIND(Res.drawable.ic_wind, Res.string.sound_wind),
    BROWN(Res.drawable.ic_brown, Res.string.sound_brown),
    PINK(Res.drawable.ic_pink, Res.string.sound_pink),
    WHITE(Res.drawable.ic_white, Res.string.sound_white),
    STREAM(Res.drawable.ic_stream, Res.string.sound_stream, pro = true),
    CRICKETS(Res.drawable.ic_crickets, Res.string.sound_crickets, pro = true),
    FAN(Res.drawable.ic_fan, Res.string.sound_fan, pro = true),
    TRAIN(Res.drawable.ic_train, Res.string.sound_train, pro = true),
    TYPING(Res.drawable.ic_typing, Res.string.sound_typing, pro = true),
    TENT(Res.drawable.ic_tent, Res.string.sound_tent, pro = true)
}
