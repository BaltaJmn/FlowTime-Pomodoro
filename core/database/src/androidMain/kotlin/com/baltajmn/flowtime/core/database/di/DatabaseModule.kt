package com.baltajmn.flowtime.core.database.di

import androidx.room.Room
import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import com.baltajmn.flowtime.core.database.datasource.MIGRATION_1_2
import com.baltajmn.flowtime.core.database.datasource.MIGRATION_3_4
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val DatabaseModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_3_4)
            .build()
    }

    single { get<AppDatabase>().sessionDao() }
    single { get<AppDatabase>().backupDao() }
    single { get<AppDatabase>().tagDao() }
    single { get<AppDatabase>().taskDao() }
}