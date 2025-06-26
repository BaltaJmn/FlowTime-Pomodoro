package com.baltajmn.flowtime.core.design.service

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AudioManager handles application lifecycle events to properly manage audio playback.
 * It pauses audio when the app goes to background and can resume when returning to foreground.
 */
class AudioManager(
    private val soundViewModel: SoundViewModel
) : DefaultLifecycleObserver {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var wasPlayingBeforeBackground = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App is in foreground
        if (wasPlayingBeforeBackground) {
            scope.launch {
                soundViewModel.resumePlayingPlayers()
            }
            wasPlayingBeforeBackground = false
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App is in background
        val hasPlayingAudio = soundViewModel.getItems().values.any { it.isPlaying }
        if (hasPlayingAudio) {
            wasPlayingBeforeBackground = true
            scope.launch {
                soundViewModel.pauseAllPlayers()
            }
        }
    }

    fun cleanup() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }
}
