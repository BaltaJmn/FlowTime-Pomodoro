package com.baltajmn.flowtime.data.goal

import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.DAILY_GOAL
import com.baltajmn.flowtime.data.fakes.FakeDataProvider
import com.baltajmn.flowtime.data.fakes.FakeSessionRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalRepositoryTest {

    private val today = LocalDate.of(2026, 9, 29)
    private val prefs = FakeDataProvider()
    private val sessions = FakeSessionRepository()
    private val days = MutableStateFlow(today)
    private val goals = GoalRepository(prefs, sessions, days)

    @Test
    fun `sin elegir nada el objetivo es de una hora`() = runTest {
        assertEquals(60, goals.goalMinutes.first())
        assertEquals(emptyList<GoalChange>(), goals.history.value)
    }

    @Test
    fun `el objetivo nuevo vale desde hoy`() = runTest {
        goals.setGoal(90, today)

        assertEquals(listOf(GoalChange(today, 90)), goals.history.value)
        assertEquals(90, goals.goalMinutes.first())
        assertEquals(60, goalOn(today.minusDays(1), goals.history.value))
    }

    @Test
    fun `cambiarlo varias veces el mismo dia deja un solo cambio`() {
        goals.setGoal(45, today.minusDays(3))
        goals.setGoal(90, today)
        goals.setGoal(30, today)
        assertEquals(listOf(GoalChange(today.minusDays(3), 45), GoalChange(today, 30)), goals.history.value)

        // Volver al de ayer no necesita ningún cambio.
        goals.setGoal(45, today)
        assertEquals(listOf(GoalChange(today.minusDays(3), 45)), goals.history.value)
    }

    @Test
    fun `de 5 en 5 y entre 10 y 480 minutos`() {
        goals.setGoal(47, today)
        assertEquals(45, goals.history.value.last().minutes)
        goals.setGoal(3, today)
        assertEquals(10, goals.history.value.last().minutes)
        goals.setGoal(1000, today)
        assertEquals(480, goals.history.value.last().minutes)
    }

    @Test
    fun `el historial se guarda en los ajustes`() {
        goals.setGoal(45, today.minusDays(3))
        goals.setGoal(90, today)

        assertEquals(goals.history.value, GoalRepository(prefs, sessions, days).history.value)
    }

    @Test
    fun `un ajuste roto se lee como si no hubiera objetivo`() {
        prefs.setString(DAILY_GOAL, "[{")

        assertEquals(emptyList<GoalChange>(), GoalRepository(prefs, sessions, days).history.value)
    }

    @Test
    fun `el progreso de hoy cambia de dia a medianoche`() = runTest {
        sessions.add(today, 45 * 60L)
        assertEquals(DayProgress(today, 45 * 60L, 60), goals.today.first())

        days.value = today.plusDays(1)
        assertEquals(DayProgress(today.plusDays(1), 0, 60), goals.today.first())
    }

    @Test
    fun `la racha se recalcula al importar dias pasados`() = runTest {
        val streaks = mutableListOf<Streak>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { goals.streak.toList(streaks) }
        sessions.add(today, 60 * 60L)
        assertEquals(1, streaks.last().current)

        // Ayer, desde una copia o un texto: hoy no cambia, pero la racha sí.
        sessions.add(today.minusDays(1), 60 * 60L)
        assertEquals(2, streaks.last().current)
    }

    @Test
    fun `una copia de seguridad trae el historial entero y se salta lo que no tiene sentido`() {
        goals.restore(
            listOf(
                GoalChange(today, 30),
                GoalChange(today.minusDays(9), 90),
                GoalChange(today.minusDays(5), 5000)
            )
        )

        assertEquals(listOf(GoalChange(today.minusDays(9), 90), GoalChange(today, 30)), goals.history.value)
    }
}
