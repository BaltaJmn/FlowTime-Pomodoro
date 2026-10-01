package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.pro.DefaultPurchasesRepository
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.timer.IosTimeSource
import com.baltajmn.flowtime.data.timer.TimeSource
import org.koin.dsl.module
import platform.Foundation.NSBundle

val DataModule = module {
    includes(RepositoriesModule)
    single(APP_VERSION) { NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "" }
    single { DocumentFiles() }
    // ponytail: sin tienda en el iPhone hasta configurar RevenueCat para iOS; sin ella, Pro no se ofrece.
    single<PurchasesRepository> { DefaultPurchasesRepository(null, get()) }
    single<TimeSource> { IosTimeSource() }
}
