package com.baltajmn.flowtime

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.data.review.calmMoments
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.goal.GoalWatcher
import com.baltajmn.flowtime.review.ReviewPrompter
import com.baltajmn.flowtime.session.SessionNotification
import com.baltajmn.flowtime.ui.FlowTimeApp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel = inject<MainViewModel>().value
    private val sessionNotification: SessionNotification by inject()
    private val appearanceRepository: AppearanceRepository by inject()
    private val goalWatcher: GoalWatcher by inject()
    private val engine: FocusEngine by inject()
    private val reviewPrompter: ReviewPrompter by inject()
    private val showSound: MutableState<Boolean> = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.enableEdgeToEdge()

        showSound.value = viewModel.getShowSound()

        setContent {
            val appearance by appearanceRepository.appearance.collectAsStateWithLifecycle()
            val celebration by goalWatcher.celebration.collectAsStateWithLifecycle()
            val dark = appearance.isDark(isSystemInDarkTheme())
            // Los iconos de las barras del sistema siguen al tema de la app, no al del sistema: con el
            // modo oscuro forzado en un móvil claro, se quedaban oscuros sobre fondo oscuro.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark }
                )
                onDispose {}
            }
            FlowTimeApp(
                appearance = appearance,
                showSound = showSound.value,
                onSoundChange = { it: Boolean -> showSound.value = it },
                celebration = celebration,
                onCelebrationShown = goalWatcher::onShown
            )
        }

        // La valoración, solo con la app a la vista y en un momento tranquilo: al terminar una sesión
        // o al cerrar la celebración del objetivo. Nunca al abrir la app ni con una sesión en marcha.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                calmMoments(
                    sessionRunning = engine.state.map { it.isActive },
                    celebrating = goalWatcher.celebration.map { it != null }
                ).collect { reviewPrompter.askIfDue(this@MainActivity) }
            }
        }
    }

    // Desde Android 14 se puede descartar; vuelve al abrir la app, y también justo después de dar
    // el permiso, que no para la actividad.
    override fun onResume() {
        super.onResume()
        sessionNotification.update()
        sessionNotification.dismissAlert()
    }

    private companion object {
        // Los mismos velos que pone enableEdgeToEdge() por defecto en la barra de navegación de botones.
        val LIGHT_SCRIM = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1B, 0x1B, 0x1B)
    }
}
