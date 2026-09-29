package com.baltajmn.flowtime

import android.app.Application
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.di.CoreModules
import com.baltajmn.flowtime.di.FeaturesModule
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@App)
            modules(
                CoreModules,
                FeaturesModule
            )
        }

        val scope = MainScope()
        val engine = get<FocusEngine>()
        val sounds = get<SoundService>()
        engine.runIn(scope)
        scope.launch {
            engine.changes.collect { change ->
                // Un cambio visto tarde (la app estaba cerrada) ya no avisa de nada.
                if (change.lateMillis > LATE_MILLIS) return@collect
                if (change.from == Phase.WORK) sounds.playConfirmationSound() else sounds.playStartSound()
            }
        }
    }

    private companion object {
        const val LATE_MILLIS = 3_000L
    }
}
