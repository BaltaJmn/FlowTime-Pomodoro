package com.baltajmn.flowtime.data.timer

interface TimeSource {
    /** Hora real: para guardar y para saber qué día es. */
    fun wallMillis(): Long

    /** Tiempo desde que se encendió el móvil. Sigue con la pantalla apagada y no cambia si se cambia la hora. */
    fun elapsedMillis(): Long

    /** Cambia en cada reinicio, que es cuando [elapsedMillis] vuelve a empezar. */
    fun bootCount(): Int
}
