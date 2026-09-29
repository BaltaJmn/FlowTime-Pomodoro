package com.baltajmn.flowtime.features.screens.settings

import com.baltajmn.flowtime.core.design.theme.Appearance
import com.baltajmn.flowtime.data.backup.Backup
import com.baltajmn.flowtime.data.backup.RestoreResult
import com.baltajmn.flowtime.data.goal.DailyGoal
import com.baltajmn.flowtime.data.goal.Streak
import com.baltajmn.flowtime.data.tag.Tag

data class SettingsState(
    val isLoading: Boolean = false,
    val userLevel: Long = 0,
    val progressPercentage: Long = 0,
    val showAlert: Boolean = true,
    val keepScreenOn: Boolean = true,
    val appearance: Appearance = Appearance(),
    val backup: BackupUiState = BackupUiState(),
    val goal: GoalUiState = GoalUiState(),
    val tags: TagsUiState = TagsUiState()
)

data class TagsUiState(
    val active: List<Tag> = emptyList(),
    val archived: List<Tag> = emptyList(),
    val message: TagMessage? = null
)

enum class TagMessage { EMPTY, DUPLICATE, LIMIT }

data class GoalUiState(val minutes: Int = DailyGoal.DEFAULT_MINUTES, val streak: Streak = Streak())

data class BackupUiState(
    val lastExportAt: Long? = null,
    /** Una copia leída y a la espera de que se confirme la importación. */
    val pending: Backup? = null,
    val message: BackupMessage? = null
)

sealed interface BackupMessage {
    data object Exported : BackupMessage
    data class Restored(val result: RestoreResult) : BackupMessage
    data object NotABackup : BackupMessage
    data object TooNew : BackupMessage
    data object ExportFailed : BackupMessage
    data object ImportFailed : BackupMessage
}
