package com.baltajmn.flowtime.session

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.FocusState

/**
 * Despierta la app al final de cada fase con cuenta atrás, aunque esté cerrada y el móvil en reposo.
 * Va con el reloj desde el arranque, no con la hora: cambiar la hora o la zona horaria no la mueve.
 * Al reiniciar el móvil el sistema la borra; [SessionReceiver] la vuelve a poner.
 */
class PhaseAlarm(
    private val context: Context,
    private val engine: FocusEngine
) {
    private val alarms = context.getSystemService(AlarmManager::class.java)

    fun schedule(state: FocusState = engine.state.value) {
        val intent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SessionReceiver::class.java).setAction(ACTION),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        if (!state.running || !state.countsDown) return alarms.cancel(intent)

        val at = SystemClock.elapsedRealtime() + engine.snapshot(state).remainingMillis
        if (canBeExact(context)) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent)
        } else {
            // Sin permiso de alarmas exactas el sistema puede retrasarla unos minutos; Ajustes lo avisa.
            alarms.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, at, intent)
        }
    }

    companion object {
        const val ACTION = "com.baltajmn.flowtime.PHASE_END"

        fun canBeExact(context: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }
}
