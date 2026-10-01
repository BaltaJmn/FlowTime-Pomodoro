package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.data.repository.SessionRepository

interface GetAllStudyTimeUseCase {
    /** Minutos de todas las sesiones. */
    suspend operator fun invoke(): Long
}

class GetAllStudyTime(
    private val sessions: SessionRepository
) : GetAllStudyTimeUseCase {

    override suspend fun invoke(): Long = sessions.totalSeconds() / 60
}
