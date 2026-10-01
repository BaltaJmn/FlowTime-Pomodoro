package com.baltajmn.flowtime.core.design.module

import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.sound.AmbientMixer
import com.baltajmn.flowtime.core.design.sound.SoundMixes
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val DesignModule: Module
    get() = module {
        single { AmbientMixer() }
        single { SoundMixes(get()) }
        single { AppearanceRepository(get()) }
        // Sin servicio que arrancar: la sesión de audio en "playback" y el modo de fondo "audio" del
        // Info.plist de iosApp lo mantienen sonando con la app en segundo plano.
        single { Ambience(get(), get()) {} }
    }
