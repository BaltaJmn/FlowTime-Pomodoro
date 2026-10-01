package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.pro.DefaultPurchasesRepository
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.pro.REVENUECAT_API_KEY
import com.baltajmn.flowtime.data.pro.RevenueCatStore
import com.baltajmn.flowtime.data.review.ReviewPolicy
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.SystemTimeSource
import com.baltajmn.flowtime.data.timer.TimeSource
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val DataModule = module {
    includes(RepositoriesModule)
    single(APP_VERSION) {
        val context = androidContext()
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }
    single { DocumentFiles(androidContext()) }
    // Crearlo configura RevenueCat; App lo pide al arrancar.
    single<PurchasesRepository> {
        DefaultPurchasesRepository(REVENUECAT_API_KEY?.let { RevenueCatStore(androidContext(), it) }, get())
    }
    single<TimeSource> { SystemTimeSource(androidContext()) }
    single {
        val context = androidContext()
        val engine = get<FocusEngine>()
        ReviewPolicy(
            dataProvider = get(),
            sessions = get(),
            sessionRunning = { engine.state.value.isActive },
            installedAt = { context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime }
        )
    }
}
