package com.baltajmn.flowtime.core.database

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * La base de datos del iPhone, con el mismo nombre que en Android, en Application Support: la carpeta
 * de los datos de la app que no ve el usuario. Nace en la versión 5, así que no lleva migraciones.
 */
fun appDatabase(): AppDatabase = Room.databaseBuilder<AppDatabase>(name = "${applicationSupport()}/app_database")
    .setDriver(BundledSQLiteDriver())
    .setQueryCoroutineContext(Dispatchers.IO)
    .build()

@OptIn(ExperimentalForeignApi::class)
private fun applicationSupport(): String = requireNotNull(
    NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )?.path
)
