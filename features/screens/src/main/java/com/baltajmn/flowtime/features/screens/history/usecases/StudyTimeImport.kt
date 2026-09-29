package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DayKeys

const val MAX_MINUTES_PER_DAY = 24 * 60L

private val importLine = Regex("""^\s*(\S+)\s*:\s*(\d+)\s*$""")

/** Días válidos del texto importado, con la clave en dígitos latinos, y cuántas líneas se descartaron. */
data class StudyTimeImport(
    val minutesByDay: Map<String, Long>,
    val ignoredLines: Int,
    val daysWithData: Int = 0
)

enum class ImportMode { REPLACE, SUM }

/**
 * Solo acepta líneas "ddMMyyyy: minutos" con una fecha real y como mucho los minutos de un día.
 * Antes se aceptaba cualquier clave y una línea como "theme_color: 1" machacaba un ajuste.
 */
fun parseStudyTimeImport(data: String): StudyTimeImport {
    val minutesByDay = linkedMapOf<String, Long>()
    var ignoredLines = 0

    data.lines().filter { it.isNotBlank() }.forEach { line ->
        val match = importLine.matchEntire(line)
        val date = match?.groupValues?.get(1)?.let(DayKeys::parse)
        val minutes = match?.groupValues?.get(2)?.toLongOrNull()

        if (date == null || minutes == null || minutes > MAX_MINUTES_PER_DAY) {
            ignoredLines++
        } else {
            minutesByDay[DayKeys.of(date)] = minutes
        }
    }

    return StudyTimeImport(minutesByDay = minutesByDay, ignoredLines = ignoredLines)
}

/** Lo que hay que guardar de cada día importado, según se sustituya o se sume a lo que ya había. */
fun mergeStudyTime(
    existing: Map<String, Long>,
    imported: Map<String, Long>,
    mode: ImportMode
): Map<String, Long> = imported.mapValues { (day, minutes) ->
    when (mode) {
        ImportMode.REPLACE -> minutes
        ImportMode.SUM -> (minutes + (existing[day] ?: 0L)).coerceAtMost(MAX_MINUTES_PER_DAY)
    }
}
