package com.baltajmn.flowtime.features.screens.history.usecases

import com.baltajmn.flowtime.core.common.extensions.mapToString
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider

interface GetStudyTimeToClipboardUseCase {
    suspend operator fun invoke(): String
}

class GetStudyTimeToClipboard(
    private val dataProvider: DataProvider
) : GetStudyTimeToClipboardUseCase {

    override suspend fun invoke(): String {
        return mapToString(dataProvider.getStudyTimeMap())
    }
}