package com.baltajmn.flowtime.session

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.text.format.DateFormat
import androidx.annotation.RequiresApi
import com.baltajmn.flowtime.core.common.extensions.formatSecondsToTime
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.TimerAction
import com.baltajmn.flowtime.widget.FocusWidget
import java.util.Date
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * El botón de los ajustes rápidos (#42): sin sesión, empieza con el último modo; con una, la para.
 * Pausar sigue en la notificación. Es un botón activo: el sistema lo repinta solo cuando se le pide
 * con [refresh], no cada segundo. Mantenerlo pulsado abre la app (QS_TILE_PREFERENCES).
 */
class FocusTileService : TileService() {

    private val engine: FocusEngine by inject()
    private val notification: SessionNotification by inject()
    private val alarm: PhaseAlarm by inject()

    override fun onStartListening() = paint()

    override fun onClick() {
        engine.perform(if (engine.state.value.isActive) TimerAction.STOP else TimerAction.START)
        // Como en SessionReceiver: con la app cerrada, no puede esperar al colector de App.
        notification.update()
        alarm.schedule()
        MainScope().launch { FocusWidget.update(applicationContext) }
        paint()
    }

    private fun paint() {
        val tile = qsTile ?: return
        val state = engine.state.value
        tile.state = if (state.isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.icon = Icon.createWithResource(this, state.mode.icon)
        tile.label = getString(state.mode.label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (state.isActive) "${getString(state.title)} · ${detail()}" else null
        }
        tile.updateTile()
    }

    /** "termina a las 10:45" con cuenta atrás, "desde las 10:20" hacia arriba, y el tiempo fijo en pausa. */
    private fun detail(): String {
        val snapshot = engine.snapshot()
        val state = snapshot.state
        if (state.isPaused) return snapshot.displaySeconds.formatSecondsToTime()
        val now = System.currentTimeMillis()
        val time = DateFormat.getTimeFormat(this)
        return if (state.countsDown) {
            getString(R.string.tile_ends_at, time.format(Date(now + snapshot.remainingMillis)))
        } else {
            getString(R.string.tile_since, time.format(Date(now - snapshot.elapsedMillis)))
        }
    }

    companion object {
        /** Tras cada cambio de la sesión. Sin el botón puesto en el panel, no hace nada. */
        fun refresh(context: Context) {
            // Algunos fabricantes lanzan si el botón no está en el panel.
            runCatching {
                requestListeningState(
                    context,
                    ComponentName(context, FocusTileService::class.java)
                )
            }
        }

        /** El diálogo del sistema para ponerlo en el panel sin editarlo a mano (Android 13 o posterior). */
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        fun requestAdd(context: Context) {
            context.getSystemService(StatusBarManager::class.java).requestAddTileService(
                ComponentName(context, FocusTileService::class.java),
                context.getString(R.string.mode_flow_time),
                Icon.createWithResource(context, R.drawable.ic_flowtime),
                context.mainExecutor
            ) {}
        }
    }
}
