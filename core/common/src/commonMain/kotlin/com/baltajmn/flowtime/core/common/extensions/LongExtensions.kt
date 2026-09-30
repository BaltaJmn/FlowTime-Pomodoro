package com.baltajmn.flowtime.core.common.extensions

fun Long.formatMinutesStudying(): String {
    if (this < 0) {
        return "Invalid duration"
    }

    val hours = this / 60
    val remainingMinutes = this % 60

    return if (hours > 0) {
        if (remainingMinutes > 0) {
            "$hours h $remainingMinutes min"
        } else {
            "$hours h"
        }
    } else {
        "$remainingMinutes min"
    }
}

fun Long.formatSecondsToTime(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val remainingSeconds = this % 60

    val clock = "${minutes.twoDigits()}:${remainingSeconds.twoDigits()}"
    return if (hours > 0) "${hours.twoDigits()}:$clock" else clock
}

/** Sin String.format, que no existe en Kotlin común; siempre con dígitos latinos. */
private fun Long.twoDigits() = toString().padStart(2, '0')