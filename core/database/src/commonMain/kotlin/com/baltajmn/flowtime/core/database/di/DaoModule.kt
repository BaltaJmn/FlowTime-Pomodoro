package com.baltajmn.flowtime.core.database.di

import com.baltajmn.flowtime.core.database.datasource.AppDatabase
import org.koin.dsl.module

/** Las tablas. La base de datos la pone el DatabaseModule de cada plataforma. */
val DaoModule = module {
    single { get<AppDatabase>().sessionDao() }
    single { get<AppDatabase>().backupDao() }
    single { get<AppDatabase>().tagDao() }
    single { get<AppDatabase>().taskDao() }
}
