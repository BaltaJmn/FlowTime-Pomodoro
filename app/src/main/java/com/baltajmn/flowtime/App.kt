package com.baltajmn.flowtime

import android.app.Application
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.core.design.service.SoundService
import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.sound.SleepTimer
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.reminder.ReminderRepository
import com.baltajmn.flowtime.data.repository.SessionRepository
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusMode
import com.baltajmn.flowtime.data.timer.Phase
import com.baltajmn.flowtime.data.timer.PhaseChange
import com.baltajmn.flowtime.di.CoreModules
import com.baltajmn.flowtime.di.FeaturesModule
import com.baltajmn.flowtime.goal.GoalWatcher
import com.baltajmn.flowtime.reminder.DailyReminder
import com.baltajmn.flowtime.session.FocusTileService
import com.baltajmn.flowtime.session.PhaseAlarm
import com.baltajmn.flowtime.session.SessionNotification
import com.baltajmn.flowtime.widget.FocusWidget
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
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

        get<SessionRepository>().importLegacyOnce()
        val engine = get<FocusEngine>()
        val notification = get<SessionNotification>()
        val alarm = get<PhaseAlarm>()
        val reminder = get<DailyReminder>()
        engine.onPhaseChange = ::alert
        val scope = MainScope()
        engine.runIn(scope)
        scope.launch {
            engine.state.collect { state ->
                notification.update(state)
                alarm.schedule(state)
                FocusTileService.refresh(this@App)
                if (state.isActive) reminder.dismiss()
            }
        }
        get<GoalWatcher>().watch(scope)
        // No molestar sigue a la sesión, y también al ajuste y a Pro. Al arrancar, por si la app se
        // cerró a mitad de una sesión.
        val focusMode = get<FocusMode>()
        scope.launch {
            combine(engine.state, focusMode.enabled, get<PurchasesRepository>().isPro) { state, _, _ -> state }
                .collect(focusMode::apply)
        }
        // Al arrancar (también tras actualizar la app) y con cada cambio en Ajustes.
        scope.launch { get<ReminderRepository>().reminder.collect { reminder.schedule() } }
        // "Al terminar la sesión" del temporizador de apagado de los sonidos (#44).
        val ambience = get<Ambience>()
        scope.launch {
            engine.state.map { it.isActive }.distinctUntilChanged().drop(1).filter { !it }.collect {
                if (ambience.sleep.value == SleepTimer.SessionEnd) ambience.fadeOutAndStop()
            }
        }
        // El widget, con cada cambio de la sesión, del progreso de hoy (también a medianoche) o del tema.
        scope.launch {
            combine(
                engine.state,
                get<GoalRepository>().today,
                get<AppearanceRepository>().appearance
            ) { _, _, _ -> }
                .collect { FocusWidget.update(this@App) }
        }
        // Configura RevenueCat. Después, sus propios avisos lo tienen al día al volver a la app.
        val purchases = get<PurchasesRepository>()
        scope.launch { purchases.refresh() }

        val tags = get<TagRepository>()
        scope.launch {
            val defaults = listOf(
                R.string.tag_study,
                R.string.tag_work,
                R.string.tag_reading,
                R.string.tag_home
            )
            tags.ensureDefaults(defaults.map(::getString))
        }
        // Renombrar la etiqueta con la sesión en marcha, o que se acaben de leer, cambia la notificación.
        scope.launch { tags.all.collect { notification.update() } }
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
        vibrate()
    }

    /** Un patrón corto, para notarlo aunque el móvil esté en vibración. En silencio, nada. */
    private fun vibrate() {
        if (getSystemService(AudioManager::class.java)?.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
        val pattern = VibrationEffect.createWaveform(VIBRATION, -1)
        vibrator?.takeIf { it.hasVibrator() }?.vibrate(pattern)
    }

    private companion object {
        // Margen para que arranque el proceso cuando la alarma lo despierta con la app cerrada.
        const val LATE_MILLIS = 60_000L
        val VIBRATION = longArrayOf(0, 120, 80, 120)
    }
}
