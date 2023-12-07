package com.baltajmn.flowtime.core.design.module

import android.media.MediaPlayer
import com.baltajmn.flowtime.core.design.service.SoundService
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val DesignModule: Module
    get() = module {
        single(named("startPlayer")) {
            MediaPlayer.create(
                androidContext(),
                com.baltajmn.flowtime.core.design.R.raw.start
            )
        }

        single(named("confirmationPlayer")) {
            MediaPlayer.create(
                androidContext(),
                com.baltajmn.flowtime.core.design.R.raw.confirmation
            )
        }

        single { SoundService(get(named("startPlayer")), get(named("confirmationPlayer"))) }
    }