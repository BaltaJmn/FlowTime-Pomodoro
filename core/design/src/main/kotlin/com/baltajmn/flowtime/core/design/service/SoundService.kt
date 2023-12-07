package com.baltajmn.flowtime.core.design.service

import android.media.MediaPlayer

class SoundService(
    private val startPlayer: MediaPlayer,
    private val confirmationPlayer: MediaPlayer,
) {

    fun playStartSound() = startPlayer.start()

    fun playConfirmationSound() = confirmationPlayer.start()

}