package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

interface GetStudyTimeUseCase {
    suspend operator fun invoke(date: LocalDate): List<Long>
}

class GetStudyTime(
    private val dataProvider: DataProvider,
) : GetStudyTimeUseCase {

    override suspend fun invoke(date: LocalDate): List<Long> {
        val studyList = mutableListOf<Long>()
        val startOfWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        for (i in 0 until 7) {
            studyList.add(dataProvider.getMinutesByDate(startOfWeek.plusDays(i.toLong())))
        }

        return studyList
    }

}