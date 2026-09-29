package com.baltajmn.flowtime.features.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baltajmn.flowtime.core.design.theme.AppTheme
import com.baltajmn.flowtime.core.design.theme.AppearanceRepository
import com.baltajmn.flowtime.core.design.theme.DarkMode
import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.KEEP_SCREEN_ON
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_ALERT
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesItem.SHOW_SOUND
import com.baltajmn.flowtime.data.backup.BackupRead
import com.baltajmn.flowtime.data.backup.BackupRepository
import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val dataProvider: DataProvider,
    private val getAllStudyTimeUseCase: GetAllStudyTimeUseCase,
    private val appearanceRepository: AppearanceRepository,
    private val backupRepository: BackupRepository,
    private val files: DocumentFiles,
    private val goals: GoalRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        getUserLevel()
        updateBackup { it.copy(lastExportAt = backupRepository.lastExportAt) }
        viewModelScope.launch {
            appearanceRepository.appearance.collect { appearance ->
                _uiState.update { it.copy(appearance = appearance) }
            }
        }
        viewModelScope.launch {
            combine(goals.goalMinutes, goals.streak, ::GoalUiState).collect { goal ->
                _uiState.update { it.copy(goal = goal) }
            }
        }
    }

    fun changeGoal(delta: Int) = goals.setGoal(goals.currentGoal + delta)

    private fun getUserLevel() {
        viewModelScope.launch {
            val userLevel = getAllStudyTimeUseCase()
            val xpBase = 100
            val xpPerHour = 50
            val xpTotal = (userLevel * xpPerHour).toInt()

            var level = 0
            var xpRequiredForCurrentLevel = 0
            var xpRequiredForNextLevel = xpBase

            while (xpTotal >= xpRequiredForNextLevel) {
                level++
                xpRequiredForCurrentLevel = xpRequiredForNextLevel
                xpRequiredForNextLevel = xpBase * (level + 1) * (level + 1)
            }

            val xpInCurrentLevel = xpTotal - xpRequiredForCurrentLevel
            val xpNeededInThisLevel = xpRequiredForNextLevel - xpRequiredForCurrentLevel
            val progressPercentage = (xpInCurrentLevel.toDouble() / xpNeededInThisLevel) * 100

            _uiState.update {
                it.copy(
                    userLevel = level.toLong(),
                    progressPercentage = progressPercentage.toLong(),
                    showAlert = dataProvider.getBoolean(SHOW_ALERT, true),
                    keepScreenOn = dataProvider.getBoolean(KEEP_SCREEN_ON, true)
                )
            }
        }
    }

    fun setTheme(theme: AppTheme) = appearanceRepository.setTheme(theme)

    fun setDarkMode(mode: DarkMode) = appearanceRepository.setDarkMode(mode)

    fun setDynamicColor(enabled: Boolean) = appearanceRepository.setDynamicColor(enabled)

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val message = runCatching {
                files.write(uri, backupRepository.export())
                backupRepository.markExported()
            }.fold(
                onSuccess = { BackupMessage.Exported },
                onFailure = { BackupMessage.ExportFailed }
            )
            updateBackup { it.copy(lastExportAt = backupRepository.lastExportAt, message = message) }
        }
    }

    /** Lee el fichero y pregunta antes de importar nada. */
    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val read = runCatching { backupRepository.read(files.read(uri)) }
                .getOrDefault(BackupRead.NotABackup)
            updateBackup {
                when (read) {
                    is BackupRead.Valid -> it.copy(pending = read.backup)
                    BackupRead.NotABackup -> it.copy(message = BackupMessage.NotABackup)
                    BackupRead.TooNew -> it.copy(message = BackupMessage.TooNew)
                }
            }
        }
    }

    fun confirmImport(withSettings: Boolean) {
        val backup = _uiState.value.backup.pending ?: return
        updateBackup { it.copy(pending = null) }
        viewModelScope.launch {
            // Sin espacio, por ejemplo. La base de datos lo hace todo o nada: no queda a medias.
            val message = runCatching { backupRepository.restore(backup, withSettings) }.fold(
                onSuccess = { BackupMessage.Restored(it) },
                onFailure = { BackupMessage.ImportFailed }
            )
            updateBackup { it.copy(message = message) }
            getUserLevel()
        }
    }

    fun cancelImport() = updateBackup { it.copy(pending = null) }

    fun onBackupMessageShown() = updateBackup { it.copy(message = null) }

    private fun updateBackup(change: (BackupUiState) -> BackupUiState) =
        _uiState.update { it.copy(backup = change(it.backup)) }

    fun saveSound(showSound: Boolean) {
        dataProvider.setBoolean(SHOW_SOUND, showSound)
    }

    fun saveAlert(showAlert: Boolean) {
        dataProvider.setBoolean(SHOW_ALERT, showAlert)
        _uiState.update {
            it.copy(
                showAlert = showAlert
            )
        }
    }

    fun saveKeepScreenOn(keepScreenOn: Boolean) {
        dataProvider.setBoolean(KEEP_SCREEN_ON, keepScreenOn)
        _uiState.update { it.copy(keepScreenOn = keepScreenOn) }
    }
}
