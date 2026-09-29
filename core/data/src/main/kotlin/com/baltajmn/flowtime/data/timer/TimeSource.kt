package com.baltajmn.flowtime.data.timer

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

interface TimeSource {
    /** Hora real: para guardar y para saber qué día es. */
    fun wallMillis(): Long

    /** Tiempo desde que se encendió el móvil. Sigue con la pantalla apagada y no cambia si se cambia la hora. */
    fun elapsedMillis(): Long

    /** Cambia en cada reinicio, que es cuando [elapsedMillis] vuelve a empezar. */
    fun bootCount(): Int
}

class SystemTimeSource(private val context: Context) : TimeSource {
    override fun wallMillis() = System.currentTimeMillis()

    override fun elapsedMillis() = SystemClock.elapsedRealtime()

    override fun bootCount() =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}
