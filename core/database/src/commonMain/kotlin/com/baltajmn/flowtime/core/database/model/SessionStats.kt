package com.baltajmn.flowtime.core.database.model

/** Los totales de un periodo. Las sesiones LEGACY suman tiempo, pero no son sesiones. */
data class PeriodTotals(val totalSeconds: Long, val sessions: Int, val sessionSeconds: Long)

/** [hour] de 0 a 23, la hora del móvil en que empezó cada sesión. */
data class HourSeconds(val hour: Int, val seconds: Long)

data class ModeSeconds(val mode: String, val seconds: Long, val sessions: Int)

data class TagSeconds(val tagId: Long?, val seconds: Long)

/** El tiempo de una tarea: la suma de sus sesiones. */
data class TaskSeconds(val taskId: Long, val seconds: Long)

/** Una tarea con su título y el tiempo que se le ha dedicado en un periodo. */
data class TaskTotal(val taskId: Long, val title: String, val seconds: Long)
