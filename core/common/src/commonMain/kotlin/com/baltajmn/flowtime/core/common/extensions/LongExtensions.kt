package com.baltajmn.flowtime.core.common.extensions

/**
 * "1 h 5 min" con las unidades cortas de cada idioma (las de CLDR, las del reloj del sistema): con "h"
 * y "min" fijos, el ruso leía "0 min из 1 h". [language] es el del móvil, el de los textos; uno que no
 * está, en inglés, como ellos.
 */
fun Long.formatMinutesStudying(language: String = systemLanguage()): String {
    if (this < 0) {
        return "Invalid duration"
    }

    val (hour, minute) = UNITS[language] ?: UNITS.getValue("en")
    val hours = this / 60
    val remainingMinutes = this % 60

    return when {
        hours == 0L -> minute.replace("#", "$remainingMinutes")
        remainingMinutes == 0L -> hour.replace("#", "$hours")
        // El japonés escribe las dos partes juntas: "1時間5分".
        else -> hour.replace("#", "$hours") + (if (language == "ja") "" else " ") + minute.replace("#", "$remainingMinutes")
    }
}

/** El idioma del móvil en dos letras ("ru"), el mismo con el que salen los textos. */
internal expect fun systemLanguage(): String

// "#" es el número. Android llama "in" al indonesio (el código antiguo); el iPhone, "id".
private val UNITS = mapOf(
    "en" to ("# h" to "# min"),
    "es" to ("# h" to "# min"),
    "fr" to ("# h" to "# min"),
    "it" to ("# h" to "# min"),
    "pt" to ("# h" to "# min"),
    "de" to ("# Std." to "# Min."),
    "nl" to ("# uur" to "# min"),
    "pl" to ("# godz." to "# min"),
    "ru" to ("# ч" to "# мин"),
    "tr" to ("# sa." to "# dk."),
    "id" to ("# jam" to "# mnt"),
    "in" to ("# jam" to "# mnt"),
    "hi" to ("# घं॰" to "# मि॰"),
    "ja" to ("#時間" to "#分"),
    "ko" to ("#시간" to "#분")
)

fun Long.formatSecondsToTime(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val remainingSeconds = this % 60

    val clock = "${minutes.twoDigits()}:${remainingSeconds.twoDigits()}"
    return if (hours > 0) "${hours.twoDigits()}:$clock" else clock
}

/** Sin String.format, que no existe en Kotlin común; siempre con dígitos latinos. */
private fun Long.twoDigits() = toString().padStart(2, '0')
