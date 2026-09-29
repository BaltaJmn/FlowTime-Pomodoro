package com.baltajmn.flowtime.core.persistence.di

import com.baltajmn.flowtime.core.persistence.sharedpreferences.DataProvider
import com.baltajmn.flowtime.core.persistence.sharedpreferences.SharedPreferencesProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

val PersistenceModule: Module
    get() = module {
        single { SharedPreferencesProvider(androidContext()) } bind DataProvider::class
    }