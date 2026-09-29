package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.data.repository.SessionRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

interface GetStudyTimeUseCase {
    /** Minutos de cada día de la semana de [date], de lunes a domingo. */
    suspend operator fun invoke(date: LocalDate): List<Long>
}

class GetStudyTime(
    private val sessions: SessionRepository
) : GetStudyTimeUseCase {

    override suspend fun invoke(date: LocalDate): List<Long> {
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = sessions.secondsByDay(monday, monday.plusDays(6))
        return (0L until 7L).map { (week[monday.plusDays(it)] ?: 0L) / 60 }
    }
}
