package com.baltajmn.flowtime.core.design.service

import android.media.MediaPlayer
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SoundService(
    private val dataProvider: DataProvider,
    private val startPlayer: MediaPlayer,
    private val confirmationPlayer: MediaPlayer,
) {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    fun playStartSound() {
        serviceScope.launch {
            try {
                val shouldPlayAlert =
                    dataProvider.getBoolean(SharedPreferencesItem.SHOW_ALERT, true)
                if (shouldPlayAlert) {
                    withContext(Dispatchers.Main) {
                        try {
                            if (!startPlayer.isPlaying) {
                                startPlayer.seekTo(0) // Reset to beginning
                                startPlayer.start()
                            }
                        } catch (e: IllegalStateException) {
                            // Player might be in invalid state, ignore
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    fun playConfirmationSound() {
        serviceScope.launch {
            try {
                val shouldPlayAlert =
                    dataProvider.getBoolean(SharedPreferencesItem.SHOW_ALERT, true)
                if (shouldPlayAlert) {
                    withContext(Dispatchers.Main) {
                        try {
                            if (!confirmationPlayer.isPlaying) {
                                confirmationPlayer.seekTo(0) // Reset to beginning
                                confirmationPlayer.start()
                            }
                        } catch (e: IllegalStateException) {
                            // Player might be in invalid state, ignore
                        }
                    }
                }
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    fun release() {
        try {
            startPlayer.release()
            confirmationPlayer.release()
        } catch (e: Exception) {
            // Ignore release errors
        }
    }
}