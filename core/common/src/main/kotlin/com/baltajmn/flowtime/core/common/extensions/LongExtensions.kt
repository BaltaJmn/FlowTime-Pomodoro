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

    val formattedTime = if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    } else {
        String.format("%02d:%02d", minutes, remainingSeconds)
    }

    return formattedTime
}