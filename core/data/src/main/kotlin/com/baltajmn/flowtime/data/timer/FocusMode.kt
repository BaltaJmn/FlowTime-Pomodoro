package com.baltajmn.flowtime.data.timer

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DND_PREVIOUS_FILTER
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.FOCUS_MODE
import com.baltajmn.flowtime.data.pro.ProFeatures
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** No molestar del sistema, detrás de una interfaz para los tests. */
interface DoNotDisturb {
    /** El acceso a No molestar, que el usuario da en los ajustes del sistema y puede quitar. */
    val granted: Boolean

    /** Android 10 o posterior: la regla propia "FlowTime", que el sistema combina con la del usuario. */
    val hasRules: Boolean

    fun setRule(active: Boolean)

    /** Android 8 y 9: el filtro global, que hay que devolver como estaba. */
    var filter: Int

    companion object {
        /** `NotificationManager.INTERRUPTION_FILTER_PRIORITY`. */
        const val PRIORITY = 2
    }
}

/**
 * No molestar mientras se trabaja (#43), de Pro: se enciende al empezar una fase de trabajo, también
 * en pausa, y se apaga en el descanso y al parar. Se llama con cada cambio de la sesión, con la app
 * abierta o cerrada, y al arrancar, por si se cerró a mitad.
 */
class FocusMode(
    private val prefs: DataProvider,
    private val system: DoNotDisturb,
    private val isPro: StateFlow<Boolean>,
    private val proEnabled: Boolean = ProFeatures.enabled
) {
    private val _enabled = MutableStateFlow(prefs.getBoolean(FOCUS_MODE, false))
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    val granted: Boolean get() = system.granted

    fun setEnabled(on: Boolean) {
        prefs.setBoolean(FOCUS_MODE, on)
        _enabled.value = on
    }

    fun apply(state: FocusState) {
        if (!system.granted) return
        val on = proEnabled && isPro.value && _enabled.value && state.phase == Phase.WORK
        if (system.hasRules) return system.setRule(on)

        // Sin reglas: se guarda el filtro que había para devolverlo tal cual. 0 es que no hay nada guardado.
        val saved = prefs.getLong(DND_PREVIOUS_FILTER)
        if (on && saved == 0L) {
            prefs.setLong(DND_PREVIOUS_FILTER, system.filter.toLong())
            system.filter = DoNotDisturb.PRIORITY
        } else if (!on && saved != 0L) {
            system.filter = saved.toInt()
            prefs.setLong(DND_PREVIOUS_FILTER, 0)
        }
    }
}
