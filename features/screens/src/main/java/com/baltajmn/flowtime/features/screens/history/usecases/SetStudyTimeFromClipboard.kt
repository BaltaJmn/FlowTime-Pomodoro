package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider

interface SetStudyTimeFromClipboardUseCase {
    /** Lee el texto y cuenta cuántos de sus días ya tienen minutos guardados, sin escribir nada. */
    suspend fun preview(data: String): StudyTimeImport

    suspend fun apply(studyTime: StudyTimeImport, mode: ImportMode)
}

class SetStudyTimeFromClipboard(
    private val dataProvider: DataProvider
) : SetStudyTimeFromClipboardUseCase {

    override suspend fun preview(data: String): StudyTimeImport {
        val parsed = parseStudyTimeImport(data)
        val existing = dataProvider.getStudyTimeMap()
        return parsed.copy(
            daysWithData = parsed.minutesByDay.keys.count { (existing[it] ?: 0L) > 0L }
        )
    }

    override suspend fun apply(studyTime: StudyTimeImport, mode: ImportMode) {
        val merged = mergeStudyTime(
            existing = dataProvider.getStudyTimeMap(),
            imported = studyTime.minutesByDay,
            mode = mode
        )
        dataProvider.setStudyTimeMap(merged)
    }
}
