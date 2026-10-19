package com.baltajmn.flowtime.data.coop

import com.baltajmn.flowtime.data.timer.Phase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomMember(val id: String, @SerialName("display_name") val displayName: String)

/**
 * La sala tal como la devuelve `my_room()` (supabase/migrations): un estado, no ticks. Las horas son
 * milisegundos desde 1970 del reloj del servidor. docs/coop.md 4.
 */
@Serializable
data class CoopRoom(
    val id: String,
    val code: String,
    val owner: String,
    @SerialName("work_ms") val workMillis: Long,
    @SerialName("break_ms") val breakMillis: Long,
    val status: String,
    @SerialName("counted_ms") val countedMillis: Long,
    @SerialName("running_since") val runningSince: Long? = null,
    @SerialName("changed_by") val changedBy: String? = null,
    @SerialName("server_now") val serverNow: Long,
    val members: List<RoomMember> = emptyList()
) {
    val cycleMillis: Long get() = workMillis + breakMillis
    val running: Boolean get() = status == RUNNING

    /** Lo contado del ciclo a la hora [serverNow] del servidor. */
    fun elapsedAt(serverNow: Long): Long =
        countedMillis + if (running && runningSince != null) (serverNow - runningSince).coerceAtLeast(0) else 0

    /**
     * La fase a esa hora, lo que lleva y lo que dura. IDLE si está parada o si lleva más de
     * [MAX_CYCLES] ciclos en marcha: la misma cuenta que hace el servidor al leerla.
     */
    fun phaseAt(serverNow: Long): RoomPhase {
        val elapsed = elapsedAt(serverNow)
        if (status == IDLE || elapsed >= MAX_CYCLES * cycleMillis) return RoomPhase(Phase.IDLE, 0, 0, 0)
        val pos = elapsed % cycleMillis
        return if (pos < workMillis) {
            RoomPhase(Phase.WORK, pos, workMillis, pos)
        } else {
            RoomPhase(Phase.BREAK, pos - workMillis, breakMillis, pos)
        }
    }

    companion object {
        const val IDLE = "idle"
        const val RUNNING = "running"
        const val MAX_CYCLES = 8
    }
}

/** [phaseElapsed] de [phaseDuration]; [cycleElapsed] es desde que empezó el trabajo de este ciclo. */
data class RoomPhase(val phase: Phase, val phaseElapsed: Long, val phaseDuration: Long, val cycleElapsed: Long)

/**
 * Cuánto va adelantado el servidor respecto a este móvil, con una llamada que salió a [sentAt] y volvió
 * a [receivedAt] (hora del móvil): se supone que contestó a mitad de camino.
 */
fun clockOffset(serverNow: Long, sentAt: Long, receivedAt: Long): Long = serverNow - (sentAt + receivedAt) / 2
