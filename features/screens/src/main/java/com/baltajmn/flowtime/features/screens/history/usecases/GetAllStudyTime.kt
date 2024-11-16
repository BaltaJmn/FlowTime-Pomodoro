package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider

interface GetAllStudyTimeUseCase {
    suspend operator fun invoke(): Long
}

class GetAllStudyTime(
    private val dataProvider: DataProvider
) : GetAllStudyTimeUseCase {

    override suspend fun invoke(): Long {
        val allDates = dataProvider.getAllDates()
        return allDates.sumOf { date -> dataProvider.getMinutesByDate(date) }
    }

}