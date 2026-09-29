package com.baltajmn.flowtime.core.persistence.model

/**
 * Configuración de cada modo cuando el usuario no ha guardado ninguna. La usan tanto los
 * temporizadores como la pantalla de ajustes, para que muestren lo mismo que se usa.
 */
object TimerDefaults {

    const val PERCENTAGE = 20L
    const val MIN_PERCENTAGE = 5L
    const val MAX_PERCENTAGE = 100L

    // Funciones y no valores: RangeModel es mutable y cada pantalla necesita su propia copia.
    fun pomodoro() = RangeModel(totalRange = 45, endRange = 45, rest = 15)

    fun flowTimeRanges() = listOf(
        RangeModel(totalRange = 15, endRange = 15, rest = 5),
        RangeModel(totalRange = 30, endRange = 15, rest = 10),
        RangeModel(totalRange = 45, endRange = 15, rest = 15)
    )

    /** Un 0 guardado es el valor por defecto de getLong, o lo dejaba guardar otro modo: sin descanso. */
    fun percentage(stored: Long): Long =
        if (stored < MIN_PERCENTAGE) PERCENTAGE else stored.coerceAtMost(MAX_PERCENTAGE)
}
