package com.baltajmn.flowtime

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.PhaseChange
import com.baltajmn.flowtime.di.CoreModules
import com.baltajmn.flowtime.di.FeaturesModule
import com.baltajmn.flowtime.session.PhaseAlarm
import com.baltajmn.flowtime.session.SessionNotification
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

        val engine = get<FocusEngine>()
        val notification = get<SessionNotification>()
        val alarm = get<PhaseAlarm>()
        engine.onPhaseChange = ::alert
        val scope = MainScope()
        engine.runIn(scope)
        scope.launch {
            engine.state.collect { state ->
                notification.update(state)
                alarm.schedule(state)
            }
        }
    }

    /** Con la app a la vista suena sin más; si no, avisa una notificación con sonido y vibración. */
    private fun alert(change: PhaseChange) {
        // Visto tarde: la alarma no pudo saltar (el móvil estaba apagado, o se forzó el cierre).
        if (change.lateMillis > LATE_MILLIS) return
        if (!get<DataProvider>().getBoolean(SharedPreferencesItem.SHOW_ALERT, true)) return
        val visible = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(
            Lifecycle.State.STARTED
        )
        if (!visible) return get<SessionNotification>().alert(change)
        val sounds = get<SoundService>()
        if (change.from == Phase.WORK) sounds.playConfirmationSound() else sounds.playStartSound()
    }

    private companion object {
        // Margen para que arranque el proceso cuando la alarma lo despierta con la app cerrada.
        const val LATE_MILLIS = 60_000L
    }
}
