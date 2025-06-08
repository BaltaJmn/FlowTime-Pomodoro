package com.baltajmn.flowtime.core.design.service

import android.media.MediaPlayer
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem

class SoundService(
    private val dataProvider: DataProvider,
    private val startPlayer: MediaPlayer,
    private val confirmationPlayer: MediaPlayer,
) {

    fun playStartSound() {
        dataProvider
            .getBoolean(SharedPreferencesItem.SHOW_ALERT, true)
            .takeIf { it }
            ?.let { playAlert ->
                startPlayer.start()
            }
    }

    fun playConfirmationSound() {
        dataProvider
            .getBoolean(SharedPreferencesItem.SHOW_ALERT, true)
            .takeIf { it }
            ?.let { playAlert ->
                confirmationPlayer.start()
            }
    }

}