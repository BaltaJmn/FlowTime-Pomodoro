package com.baltajmn.flowtime.core.design.service

import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Extension functions for MediaPlayer to provide safer and more convenient operations
 */

/**
 * Safely starts the MediaPlayer with error handling
 */
suspend fun MediaPlayer.safeStart(): Boolean = withContext(Dispatchers.Main) {
    try {
        if (!isPlaying) {
            start()
            true
        } else {
            true
        }
    } catch (e: IllegalStateException) {
        false
    }
}

/**
 * Safely pauses the MediaPlayer with error handling
 */
suspend fun MediaPlayer.safePause(): Boolean = withContext(Dispatchers.Main) {
    try {
        if (isPlaying) {
            pause()
        }
        true
    } catch (e: IllegalStateException) {
        false
    }
}

/**
 * Safely stops the MediaPlayer with error handling
 */
suspend fun MediaPlayer.safeStop(): Boolean = withContext(Dispatchers.Main) {
    try {
        if (isPlaying) {
            stop()
        }
        true
    } catch (e: IllegalStateException) {
        false
    }
}

/**
 * Safely releases the MediaPlayer with error handling
 */
suspend fun MediaPlayer.safeRelease() = withContext(Dispatchers.Main) {
    try {
        release()
    } catch (e: Exception) {
        // Ignore release errors
    }
}

/**
 * Safely sets volume with error handling
 */
suspend fun MediaPlayer.safeSetVolume(leftVolume: Float, rightVolume: Float): Boolean =
    withContext(Dispatchers.Main) {
        try {
            setVolume(leftVolume, rightVolume)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

/**
 * Safely seeks to position with error handling
 */
suspend fun MediaPlayer.safeSeekTo(position: Int): Boolean = withContext(Dispatchers.Main) {
    try {
        seekTo(position)
        true
    } catch (e: IllegalStateException) {
        false
    }
}

/**
 * Waits for MediaPlayer to be prepared with timeout
 */
suspend fun MediaPlayer.waitForPrepared(timeoutMs: Long = 5000): Boolean {
    return withTimeoutOrNull(timeoutMs) {
        var attempts = 0
        val maxAttempts = (timeoutMs / 50).toInt()

        while (attempts < maxAttempts) {
            try {
                if (isPlaying || !isLooping) {
                    // If it's playing or not looping, it's prepared
                    return@withTimeoutOrNull true
                }
                kotlinx.coroutines.delay(50)
                attempts++
            } catch (e: IllegalStateException) {
                return@withTimeoutOrNull false
            }
        }
        false
    } ?: false
}

/**
 * Checks if MediaPlayer is in a valid state
 */
fun MediaPlayer.isInValidState(): Boolean {
    return try {
        // Try to get current position - this will throw if in invalid state
        currentPosition
        true
    } catch (e: IllegalStateException) {
        false
    }
}

/**
 * Safely resets MediaPlayer to idle state
 */
suspend fun MediaPlayer.safeReset(): Boolean = withContext(Dispatchers.Main) {
    try {
        reset()
        true
    } catch (e: IllegalStateException) {
        false
    }
}
