package com.baltajmn.flowtime.features.screens.fakes

import com.baltajmn.flowtime.data.backup.Backup
import com.baltajmn.flowtime.data.backup.BackupRead
import com.baltajmn.flowtime.data.backup.BackupRepository
import com.baltajmn.flowtime.data.backup.RestoreResult

/** Sin ficheros: a las pantallas solo les llega la fecha de la última copia. */
class FakeBackups(override var lastExportAt: Long? = null) : BackupRepository {

    override suspend fun export() = ""

    override fun markExported() = Unit

    override suspend fun read(text: String): BackupRead = BackupRead.NotABackup

    override suspend fun restore(backup: Backup, withSettings: Boolean) = RestoreResult(0, 0, 0)
}
