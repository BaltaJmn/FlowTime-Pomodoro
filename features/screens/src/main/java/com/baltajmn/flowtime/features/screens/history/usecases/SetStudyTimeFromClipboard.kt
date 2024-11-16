package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.core.common.extensions.stringToMap
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider

interface SetStudyTimeFromClipboardUseCase {
    suspend operator fun invoke(data: String)
}

class SetStudyTimeFromClipboard(
    private val dataProvider: DataProvider
) : SetStudyTimeFromClipboardUseCase {

    override suspend fun invoke(data: String) {
        dataProvider.setStudyTimeMap(stringToMap(data))
    }
}