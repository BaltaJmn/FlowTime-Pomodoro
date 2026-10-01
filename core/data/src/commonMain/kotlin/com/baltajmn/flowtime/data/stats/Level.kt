package com.baltajmn.flowtime.data.stats

/** El nivel y cuánto falta para el siguiente, en %. */
data class Level(val level: Long, val progressPercentage: Long)

/**
 * El nivel por los minutos de toda la vida, el mismo cálculo que tenía Ajustes: 100 puntos para el
 * nivel 1 y 100·n² para el n; cada minuto da 50 (la variable se llamaba "por hora", pero recibe
 * minutos). No se cambia, para no bajarle el nivel a nadie.
 */
fun level(totalMinutes: Long): Level {
    val xpTotal = totalMinutes * XP_PER_MINUTE
    var level = 0L
    var current = 0L
    var next = XP_BASE
    while (xpTotal >= next) {
        level++
        current = next
        next = XP_BASE * (level + 1) * (level + 1)
    }
    val progress = (xpTotal - current).toDouble() / (next - current) * 100
    return Level(level, progress.toLong())
}

private const val XP_BASE = 100L
private const val XP_PER_MINUTE = 50L
