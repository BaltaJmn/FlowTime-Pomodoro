package com.baltajmn.flowtime.core.design.service

import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaPlayer
import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class SoundViewModel(
    private val dataProvider: DataProvider
) : ViewModel() {

    // Thread-safe map for MediaPlayer instances
    private val players: ConcurrentHashMap<PlayerType, MediaPlayer> = ConcurrentHashMap()

    // Thread-safe map for tracking preparation state
    private val preparingPlayers: ConcurrentHashMap<PlayerType, Boolean> = ConcurrentHashMap()

    private val _uiState = MutableStateFlow(SoundState())
    val uiState: StateFlow<SoundState> = _uiState.asStateFlow()

    init {
        initializeSoundState()
    }

    private fun initializeSoundState() {
        viewModelScope.launch(Dispatchers.IO) {
            val soundMap = mutableMapOf<PlayerType, PlayerState>()

            PlayerType.entries.forEach { type ->
                val volume = dataProvider.getFloat(type.name, 0f)
                soundMap[type] = PlayerState(volume = volume, isPlaying = false)
            }

            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    state.copy(soundMap = soundMap)
                }
            }
        }
    }

    fun getItems(): Map<PlayerType, PlayerState> = _uiState.value.soundMap

    fun muteAll() {
        viewModelScope.launch(Dispatchers.IO) {
            PlayerType.entries.forEach { type ->
                controlSounds(type, false)
            }
        }
    }

    fun controlSounds(playerType: PlayerType, playing: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (playing) {
                    startPlayer(playerType)
                } else {
                    pausePlayer(playerType)
                }

                withContext(Dispatchers.Main) {
                    updateIsPlaying(playerType, playing)
                }
            } catch (e: Exception) {
                // Log error and update UI state
                withContext(Dispatchers.Main) {
                    updateIsPlaying(playerType, false)
                }
            }
        }
    }

    private suspend fun startPlayer(playerType: PlayerType) {
        val player = getOrCreatePlayer(playerType)
        if (player.isPlaying) return

        // Wait for preparation if needed with timeout
        var attempts = 0
        while (preparingPlayers[playerType] == true && attempts < AudioConfig.MAX_RETRY_ATTEMPTS) {
            kotlinx.coroutines.delay(AudioConfig.PREPARATION_CHECK_DELAY_MS)
            attempts++
        }

        // Check if player is in valid state before starting
        if (player.isInValidState()) {
            val success = player.safeStart()
            if (!success) {
                // Player failed to start, recreate it
                recreatePlayer(playerType)
            }
        } else {
            // Player is in invalid state, recreate it
            recreatePlayer(playerType)
        }
    }

    private suspend fun pausePlayer(playerType: PlayerType) {
        players[playerType]?.let { player ->
            if (player.isInValidState()) {
                player.safePause()
            }
        }
    }

    fun setVolume(type: PlayerType, volume: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            // Save to preferences in background
            dataProvider.setFloat(type.name, volume)

            // Update player volume
            players[type]?.setVolume(volume, volume)

            // Update UI state on main thread
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val newSoundMap = state.soundMap.toMutableMap()
                    newSoundMap[type] = newSoundMap[type]?.copy(volume = volume)
                        ?: PlayerState(volume = volume, isPlaying = false)
                    state.copy(soundMap = newSoundMap)
                }
            }
        }
    }

    private fun updateIsPlaying(playerType: PlayerType, playing: Boolean) {
        _uiState.update { current ->
            val newSoundMap = current.soundMap.toMutableMap()
            newSoundMap[playerType] = newSoundMap[playerType]?.copy(isPlaying = playing)
                ?: PlayerState(volume = 0f, isPlaying = playing)
            current.copy(soundMap = newSoundMap)
        }
    }

    private suspend fun getOrCreatePlayer(playerType: PlayerType): MediaPlayer {
        return players[playerType] ?: createPlayer(playerType)
    }

    private suspend fun createPlayer(type: PlayerType): MediaPlayer {
        preparingPlayers[type] = true

        return withContext(Dispatchers.Main) {
            MediaPlayer().apply {
                val selectedVolume = _uiState.value.soundMap[type]?.volume ?: 0f

                isLooping = true
                setAudioStreamType(STREAM_MUSIC)

                try {
                    setDataSource("https://mynoise.world/NoisesOnline/Audio/${type.sound}.ogg")
                    setVolume(selectedVolume, selectedVolume)

                    setOnPreparedListener { mp ->
                        preparingPlayers[type] = false
                        // Don't auto-start, let user control
                    }

                    setOnErrorListener { mp, what, extra ->
                        preparingPlayers[type] = false
                        // Handle error gracefully, try to recreate player
                        viewModelScope.launch(Dispatchers.IO) {
                            recreatePlayer(type)
                        }
                        true
                    }

                    setOnCompletionListener { mp ->
                        // Update UI state when playback completes
                        viewModelScope.launch(Dispatchers.Main) {
                            updateIsPlaying(type, false)
                        }
                    }

                    setOnBufferingUpdateListener { mp, percent ->
                        // Could be used to show buffering progress in UI
                    }

                    prepareAsync()
                    players[type] = this
                } catch (e: Exception) {
                    preparingPlayers[type] = false
                    throw e
                }
            }
        }
    }

    private suspend fun recreatePlayer(playerType: PlayerType) {
        // Release old player safely
        players[playerType]?.let { player ->
            player.safeRelease()
        }
        players.remove(playerType)
        preparingPlayers.remove(playerType)

        // Create new player
        try {
            createPlayer(playerType)
        } catch (e: Exception) {
            // If recreation fails, update UI to reflect failure
            withContext(Dispatchers.Main) {
                updateIsPlaying(playerType, false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch(Dispatchers.IO) {
            releaseAllPlayers()
        }
    }

    private suspend fun releaseAllPlayers() {
        // Stop and release all players safely
        players.values.forEach { player ->
            player.safeStop()
            player.safeRelease()
        }
        players.clear()
        preparingPlayers.clear()
    }

    fun pauseAllPlayers() {
        viewModelScope.launch(Dispatchers.IO) {
            players.values.forEach { player ->
                if (player.isInValidState()) {
                    player.safePause()
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update { current ->
                    val newSoundMap = current.soundMap.toMutableMap()
                    newSoundMap.keys.forEach { type ->
                        newSoundMap[type] = newSoundMap[type]?.copy(isPlaying = false)
                            ?: PlayerState(volume = 0f, isPlaying = false)
                    }
                    current.copy(soundMap = newSoundMap)
                }
            }
        }
    }

    fun resumePlayingPlayers() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value.soundMap.forEach { (type, state) ->
                if (state.isPlaying) {
                    startPlayer(type)
                }
            }
        }
    }

    fun setVolumeWithFade(type: PlayerType, targetVolume: Float) {
        viewModelScope.launch(Dispatchers.IO) {
            val player = players[type]
            if (player != null && player.isInValidState()) {
                val currentVolume = _uiState.value.soundMap[type]?.volume ?: 0f
                val steps = AudioConfig.VOLUME_FADE_STEPS
                val stepDelay = AudioConfig.VOLUME_FADE_DURATION_MS / steps
                val volumeStep = (targetVolume - currentVolume) / steps

                repeat(steps) { step ->
                    val newVolume = currentVolume + (volumeStep * (step + 1))
                    player.safeSetVolume(newVolume, newVolume)
                    kotlinx.coroutines.delay(stepDelay)
                }
            }

            // Update final volume
            setVolume(type, targetVolume)
        }
    }
}

data class SoundState(
    val soundMap: Map<PlayerType, PlayerState> = mapOf(
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
    val volume: Float = 0f,
    val isPlaying: Boolean = false
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
