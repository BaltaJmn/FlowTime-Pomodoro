package com.baltajmn.flowtime.data.goal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

class StreaksTest {

    // Del lunes 21 al domingo 27 de septiembre de 2026 es una semana entera.
    private val monday = LocalDate(2026, 9, 21)

    private fun day(offset: Int): LocalDate = monday.plus(offset, DateTimeUnit.DAY)

    /** Minutos de cada día, contando desde el lunes 21. */
    private fun days(vararg minutes: Int) =
        minutes.withIndex().associate { (offset, value) -> day(offset) to value * 60L }

    @Test
    fun `sin sesiones no hay racha`() {
        assertEquals(Streak(), streak(emptyMap(), emptyList(), today = monday))
    }

    @Test
    fun `hoy sin cumplir no rompe la racha hasta que acaba el dia`() {
        val secondsByDay = days(60, 70, 90, 10)

        assertEquals(
            Streak(current = 3, best = 3, todayMet = false, freeDayUsedThisWeek = false),
            streak(secondsByDay, emptyList(), today = day(3))
        )
    }

    @Test
    fun `hoy suma en cuanto se cumple`() {
        val today = streak(days(60, 70, 90, 60), emptyList(), today = day(3))

        assertEquals(4, today.current)
        assertTrue(today.todayMet)
    }

    @Test
    fun `un dia libre por semana se perdona y el segundo rompe la racha`() {
        // El martes no llega, pero es el único de la semana.
        val oneMiss = streak(days(60, 20, 60, 60, 60), emptyList(), today = day(4))
        assertEquals(Streak(current = 4, best = 4, todayMet = true, freeDayUsedThisWeek = true), oneMiss)

        // El jueves tampoco: la racha vuelve a empezar el viernes.
        val twoMisses = streak(days(60, 20, 60, 0, 60), emptyList(), today = day(4))
        assertEquals(Streak(current = 1, best = 2, todayMet = true, freeDayUsedThisWeek = false), twoMisses)
    }

    @Test
    fun `ayer sin cumplir ya gasta el dia libre de la semana`() {
        // Si hoy tampoco llega, la racha se rompe: la pantalla puede avisar.
        val streak = streak(days(60, 60, 20), emptyList(), today = day(3))

        assertEquals(2, streak.current)
        assertTrue(streak.freeDayUsedThisWeek)
    }

    @Test
    fun `el dia libre es de cada semana`() {
        // El domingo 27 y el lunes 28 fallan, pero son de semanas distintas.
        val secondsByDay = days(60, 60, 60, 60, 60, 60, 0, 0, 60)

        assertEquals(7, streak(secondsByDay, emptyList(), today = day(8)).current)
    }

    @Test
    fun `los dias sin cumplir antes de la racha no gastan el dia libre`() {
        val streak = streak(days(20, 20, 60, 60), emptyList(), today = day(3))

        assertEquals(2, streak.current)
        assertFalse(streak.freeDayUsedThisWeek)
    }

    @Test
    fun `cambiar el objetivo no cambia como se midieron los dias anteriores`() {
        // Hasta el martes, 60 minutos; desde el miércoles, 120.
        val history = listOf(GoalChange(day(2), 120))

        assertEquals(60, goalOn(day(1), history))
        assertEquals(120, goalOn(day(2), history))
        assertEquals(4, streak(days(60, 60, 120, 120), history, today = day(3)).current)
        // Con el objetivo nuevo, 90 minutos ya no llegan: el jueves es el día libre.
        assertEquals(4, streak(days(60, 60, 120, 90, 120), history, today = day(4)).current)
    }

    @Test
    fun `la mejor racha se queda aunque luego haya huecos`() {
        val secondsByDay = days(60, 60, 60, 60, 60, 0, 0, 60, 60)

        assertEquals(
            Streak(current = 2, best = 5, todayMet = true, freeDayUsedThisWeek = false),
            streak(secondsByDay, emptyList(), today = day(8))
        )
    }

    @Test
    fun `cada dia cuenta con las sesiones que empezaron ese dia`() {
        // Una sesión de 23:30 a 00:30 cuenta entera para el día en que empezó: el siguiente no llega.
        val secondsByDay = mapOf(day(0) to 60 * 60L, day(1) to 30 * 60L)
        val streak = streak(secondsByDay, emptyList(), today = day(1))

        assertEquals(1, streak.current)
        assertFalse(streak.todayMet)
    }
}
