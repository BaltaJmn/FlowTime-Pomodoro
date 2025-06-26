package com.baltajmn.flowtime.core.design.service

/**
 * Configuration constants for audio performance optimization
 */
object AudioConfig {

    // MediaPlayer preparation timeout
    const val PREPARATION_TIMEOUT_MS = 10000L

    // Delay between preparation checks
    const val PREPARATION_CHECK_DELAY_MS = 100L

    // Maximum number of concurrent MediaPlayer instances
    const val MAX_CONCURRENT_PLAYERS = 12

    // Buffer size for audio streaming
    const val AUDIO_BUFFER_SIZE = 8192

    // Retry attempts for failed operations
    const val MAX_RETRY_ATTEMPTS = 3

    // Delay between retry attempts
    const val RETRY_DELAY_MS = 500L

    // Volume fade duration in milliseconds
    const val VOLUME_FADE_DURATION_MS = 300L

    // Volume fade steps
    const val VOLUME_FADE_STEPS = 10

    // Cache size for audio URLs (in MB)
    const val AUDIO_CACHE_SIZE_MB = 50L

    // Audio quality settings
    object Quality {
        const val SAMPLE_RATE = 44100
        const val BIT_RATE = 128000
        const val CHANNELS = 2
    }

    // Network settings for audio streaming
    object Network {
        const val CONNECTION_TIMEOUT_MS = 15000
        const val READ_TIMEOUT_MS = 30000
        const val WRITE_TIMEOUT_MS = 15000
    }

    // Performance settings
    object Performance {
        // Use background thread for audio operations
        const val USE_BACKGROUND_THREAD = true

        // Pre-load audio files
        const val PRELOAD_AUDIO = false

        // Use audio focus
        const val USE_AUDIO_FOCUS = true

        // Enable low latency mode
        const val LOW_LATENCY_MODE = true
    }
}
