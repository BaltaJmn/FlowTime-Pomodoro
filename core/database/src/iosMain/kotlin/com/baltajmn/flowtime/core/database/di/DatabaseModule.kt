package com.baltajmn.flowtime.core.database.di

import com.baltajmn.flowtime.core.database.appDatabase
import org.koin.dsl.module

val DatabaseModule = module {
    single { appDatabase() }
    includes(DaoModule)
}
