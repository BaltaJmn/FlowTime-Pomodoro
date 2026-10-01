package com.baltajmn.flowtime.core.persistence.di

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.UserDefaultsProvider
import org.koin.core.module.Module
import org.koin.dsl.module

val PersistenceModule: Module
    get() = module {
        single<DataProvider> { UserDefaultsProvider() }
    }
