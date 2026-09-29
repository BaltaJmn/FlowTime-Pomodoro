package com.baltajmn.flowtime.features.screens.pomodoro

import org.junit.Assert.assertEquals
import org.junit.Test

class PomodoroMinutesTest {

    @Test
    fun `un pomodoro terminado cuenta entero`() {
        assertEquals(
            45L,
            pomodoroMinutesWorked(workMinutes = 45, remainingSeconds = 0, completed = true)
        )
    }

    @Test
    fun `al parar a medias cuenta los minutos completos que llevaba`() {
        // Quedaban 34:30 de 45:00: llevaba 10:30.
        val minutes = pomodoroMinutesWorked(
            workMinutes = 45,
            remainingSeconds = 34 * 60L + 30,
            completed = false
        )

        assertEquals(10L, minutes)
    }

    @Test
    fun `parar antes del primer minuto no cuenta nada`() {
        val minutes = pomodoroMinutesWorked(
            workMinutes = 45,
            remainingSeconds = 45 * 60L - 30,
            completed = false
        )

        assertEquals(0L, minutes)
    }

    @Test
    fun `parar en el descanso no vuelve a contar el trabajo`() {
        // Al empezar el descanso el tiempo restante vuelve a la duración completa.
        val minutes = pomodoroMinutesWorked(
            workMinutes = 45,
            remainingSeconds = 45 * 60L,
            completed = false
        )

        assertEquals(0L, minutes)
    }
}
