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
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.tag.Tag
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.tag.TagResult
import com.baltajmn.flowtime.features.screens.pro.ProAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val dataProvider: DataProvider,
    private val appearanceRepository: AppearanceRepository,
    private val backupRepository: BackupRepository,
    private val files: DocumentFiles,
    private val goals: GoalRepository,
    private val tags: TagRepository,
    private val purchases: PurchasesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    init {
        loadSwitches()
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
        viewModelScope.launch {
            combine(tags.active, tags.archived, ::Pair).collect { (active, archived) ->
                updateTags { it.copy(active = active, archived = archived) }
            }
        }
        viewModelScope.launch {
            combine(purchases.isPro, purchases.isSupporter, ::Pair).collect { (pro, supporter) ->
                updatePurchases { it.copy(pro = ProAccess.of(pro), isSupporter = supporter) }
            }
        }
    }

    fun restorePurchases() {
        updatePurchases { it.copy(restoring = true) }
        viewModelScope.launch {
            val found = purchases.restore()
            updatePurchases {
                it.copy(
                    restoring = false,
                    message = if (found) RestoreMessage.RESTORED else RestoreMessage.NOTHING
                )
            }
        }
    }

    fun onRestoreMessageShown() = updatePurchases { it.copy(message = null) }

    private fun updatePurchases(change: (PurchasesUiState) -> PurchasesUiState) =
        _uiState.update { it.copy(purchases = change(it.purchases)) }

    fun addTag(name: String) = changeTags { tags.create(name) }

    fun renameTag(id: Long, name: String) = changeTags { tags.rename(id, name) }

    /** El siguiente color de la paleta. */
    fun recolorTag(tag: Tag) {
        viewModelScope.launch { tags.recolor(tag.id, tag.color + 1) }
    }

    fun archiveTag(id: Long) {
        viewModelScope.launch { tags.archive(id) }
    }

    fun unarchiveTag(id: Long) = changeTags { tags.unarchive(id) }

    fun onTagMessageShown() = updateTags { it.copy(message = null) }

    private fun changeTags(change: suspend () -> TagResult) {
        viewModelScope.launch {
            val message = when (change()) {
                is TagResult.Done -> null
                TagResult.Invalid -> TagMessage.EMPTY
                TagResult.Duplicate -> TagMessage.DUPLICATE
                TagResult.LimitReached -> TagMessage.LIMIT
            }
            updateTags { it.copy(message = message) }
        }
    }

    private fun updateTags(change: (TagsUiState) -> TagsUiState) =
        _uiState.update { it.copy(tags = change(it.tags)) }

    fun changeGoal(delta: Int) = goals.setGoal(goals.currentGoal + delta)

    private fun loadSwitches() = _uiState.update {
        it.copy(
            showAlert = dataProvider.getBoolean(SHOW_ALERT, true),
            keepScreenOn = dataProvider.getBoolean(KEEP_SCREEN_ON, true)
        )
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
            // Con los ajustes de la copia, los interruptores pueden haber cambiado.
            loadSwitches()
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
