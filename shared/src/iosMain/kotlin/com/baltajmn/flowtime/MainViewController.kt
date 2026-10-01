package com.baltajmn.flowtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.database.di.DatabaseModule
import com.baltajmn.flowtime.core.design.module.DesignModule
import com.baltajmn.flowtime.core.design.resources.Res
import com.baltajmn.flowtime.core.design.resources.tag_home
import com.baltajmn.flowtime.core.design.resources.tag_reading
import com.baltajmn.flowtime.core.design.resources.tag_study
import com.baltajmn.flowtime.core.design.resources.tag_work
import com.baltajmn.flowtime.core.design.sound.Ambience
import com.baltajmn.flowtime.core.design.sound.SleepTimer
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.core.persistence.di.PersistenceModule
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND
import com.baltajmn.flowtime.data.di.DataModule
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.features.screens.di.ScreensModule
import com.baltajmn.flowtime.features.screens.pro.ProLauncher
import com.baltajmn.flowtime.features.screens.settings.AppIcons
import com.baltajmn.flowtime.ui.FlowTimeApp
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.koin.compose.koinInject
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.UIKit.UIViewController

/** La app entera en el iPhone: iosApp la pone como pantalla principal. */
@Suppress("ktlint:standard:function-naming", "FunctionName")
fun MainViewController(): UIViewController {
    koin
    return ComposeUIViewController { IosApp() }
}

// Una sola vez, aunque iOS vuelva a crear la pantalla.
private val koin: Koin by lazy {
    val koin = startKoin {
        modules(
            PersistenceModule,
            DatabaseModule,
            DesignModule,
            DataModule,
            ScreensModule,
            module { single { AppIcons() } }
        )
    }.koin
    start(koin)
    koin
}

/** Lo que en Android hace App al arrancar, menos lo que el iPhone aún no tiene. */
private fun start(koin: Koin) {
    val engine = koin.get<FocusEngine>()
    val scope = MainScope()
    engine.runIn(scope)
    // "Al terminar la sesión" del temporizador de apagado de los sonidos (#44).
    val ambience = koin.get<Ambience>()
    scope.launch {
        engine.state.map { it.isActive }.distinctUntilChanged().drop(1).filter { !it }.collect {
            if (ambience.sleep.value == SleepTimer.SessionEnd) ambience.fadeOutAndStop()
        }
    }
    val tags = koin.get<TagRepository>()
    scope.launch {
        val defaults = listOf(Res.string.tag_study, Res.string.tag_work, Res.string.tag_reading, Res.string.tag_home)
        tags.ensureDefaults(defaults.map { getString(it) })
    }
}

@Composable
private fun IosApp() {
    val appearance by koinInject<AppearanceRepository>().appearance.collectAsStateWithLifecycle()
    val proLauncher = koinInject<ProLauncher>()
    val proRequest by proLauncher.request.collectAsStateWithLifecycle()
    val dataProvider = koinInject<DataProvider>()
    var showSound by remember { mutableStateOf(dataProvider.getBoolean(SHOW_SOUND)) }
    FlowTimeApp(
        appearance = appearance,
        showSound = showSound,
        onSoundChange = { showSound = it },
        proRequest = proRequest,
        onProClosed = proLauncher::close
    )
}
