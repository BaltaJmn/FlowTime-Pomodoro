package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.pro.DefaultPurchasesRepository
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.pro.REVENUECAT_IOS_API_KEY
import com.baltajmn.flowtime.data.pro.RevenueCatStore
import com.baltajmn.flowtime.data.review.ReviewPolicy
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.IosTimeSource
import com.baltajmn.flowtime.data.timer.TimeSource
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.dsl.module
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileCreationDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.timeIntervalSince1970

val DataModule = module {
    includes(RepositoriesModule)
    single(APP_VERSION) { NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "" }
    single { DocumentFiles() }
    // Crearlo configura RevenueCat; MainViewController lo pide al arrancar.
    single<PurchasesRepository> { DefaultPurchasesRepository(REVENUECAT_IOS_API_KEY?.let(::RevenueCatStore), get()) }
    single<TimeSource> { IosTimeSource() }
    single {
        val engine = get<FocusEngine>()
        ReviewPolicy(
            dataProvider = get(),
            sessions = get(),
            sessionRunning = { engine.state.value.isActive },
            installedAt = ::installedAt
        )
    }
}

/** Cuándo se instaló la app: iOS crea Documents al instalarla. Sin saberlo, como si fuera hace mucho. */
@OptIn(ExperimentalForeignApi::class)
private fun installedAt(): Long {
    val manager = NSFileManager.defaultManager
    val documents = manager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask).firstOrNull() as? NSURL
    val created = documents?.path?.let { manager.attributesOfItemAtPath(it, null)?.get(NSFileCreationDate) } as? NSDate
    return created?.let { (it.timeIntervalSince1970 * 1000).toLong() } ?: 0L
}
