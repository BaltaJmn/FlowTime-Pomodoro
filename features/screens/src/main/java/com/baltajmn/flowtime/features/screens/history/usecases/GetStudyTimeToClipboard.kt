package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.core.common.extensions.mapToString
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DayKeys
import com.baltajmn.flowtime.data.repository.SessionRepository
import kotlinx.datetime.LocalDate

interface GetStudyTimeToClipboardUseCase {
    suspend operator fun invoke(): String
}

/** El mismo texto de siempre, "ddMMyyyy: minutos" por día, ahora sacado de las sesiones. */
class GetStudyTimeToClipboard(
    private val sessions: SessionRepository
) : GetStudyTimeToClipboardUseCase {

    override suspend fun invoke(): String = mapToString(
        // Las fechas se comparan como texto yyyy-MM-dd: los límites tienen que tener cuatro cifras de año.
        sessions.secondsByDay(LocalDate(1970, 1, 1), LocalDate(9999, 12, 31))
            .entries
            .sortedBy { it.key }
            .associate { (day, seconds) -> DayKeys.of(day) to seconds / 60 }
    )
}
