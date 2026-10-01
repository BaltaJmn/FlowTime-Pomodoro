package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.pro.DefaultPurchasesRepository
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.pro.REVENUECAT_IOS_API_KEY
import com.baltajmn.flowtime.data.pro.RevenueCatStore
import com.baltajmn.flowtime.data.timer.IosTimeSource
import com.baltajmn.flowtime.data.timer.TimeSource
import org.koin.dsl.module
import platform.Foundation.NSBundle

val DataModule = module {
    includes(RepositoriesModule)
    single(APP_VERSION) { NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "" }
    single { DocumentFiles() }
    // Crearlo configura RevenueCat; MainViewController lo pide al arrancar.
    single<PurchasesRepository> { DefaultPurchasesRepository(REVENUECAT_IOS_API_KEY?.let(::RevenueCatStore), get()) }
    single<TimeSource> { IosTimeSource() }
}
