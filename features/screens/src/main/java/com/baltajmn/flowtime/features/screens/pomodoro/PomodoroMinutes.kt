package com.baltajmn.flowtime.features.screens.pomodoro

/** Minutos completos de un Pomodoro: todos si terminó, y si se paró a medias, los que llevaba. */
fun pomodoroMinutesWorked(workMinutes: Int, remainingSeconds: Long, completed: Boolean): Long =
    if (completed) {
        workMinutes.toLong()
    } else {
        ((workMinutes * 60L - remainingSeconds) / 60).coerceAtLeast(0)
    }
