package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.data.repository.SessionRepository

interface SetStudyTimeFromClipboardUseCase {
    /** Lee el texto y cuenta cuántos de sus días ya tienen tiempo guardado, sin escribir nada. */
    suspend fun preview(data: String): StudyTimeImport

    suspend fun apply(studyTime: StudyTimeImport, mode: ImportMode)
}

/** Lo importado entra como sesiones LEGACY: el texto solo trae minutos por día. */
class SetStudyTimeFromClipboard(
    private val sessions: SessionRepository
) : SetStudyTimeFromClipboardUseCase {

    override suspend fun preview(data: String): StudyTimeImport {
        val parsed = parseStudyTimeImport(data)
        val days = parsed.minutesByDay.keys
        if (days.isEmpty()) return parsed
        val existing = sessions.secondsByDay(days.min(), days.max())
        return parsed.copy(daysWithData = days.count { (existing[it] ?: 0L) > 0L })
    }

    override suspend fun apply(studyTime: StudyTimeImport, mode: ImportMode) = sessions.addToDays(
        secondsByDay = studyTime.minutesByDay.mapValues { it.value * 60 },
        replace = mode == ImportMode.REPLACE
    )
}
