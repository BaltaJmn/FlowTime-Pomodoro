package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.backup.BackupRepository
import com.baltajmn.flowtime.data.backup.DefaultBackupRepository
import com.baltajmn.flowtime.data.backup.DocumentFiles
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.pro.DefaultPurchasesRepository
import com.baltajmn.flowtime.data.pro.ProGate
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.pro.REVENUECAT_API_KEY
import com.baltajmn.flowtime.data.pro.RevenueCatStore
import com.baltajmn.flowtime.data.repository.DefaultSessionRepository
import com.baltajmn.flowtime.data.repository.SessionRepository
import com.baltajmn.flowtime.data.review.ReviewPolicy
import com.baltajmn.flowtime.data.stats.DefaultStatsRepository
import com.baltajmn.flowtime.data.stats.StatsRepository
import com.baltajmn.flowtime.data.tag.DefaultTagRepository
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.task.DefaultTaskRepository
import com.baltajmn.flowtime.data.task.TaskRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.SystemTimeSource
import com.baltajmn.flowtime.data.timer.TimeSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val DataModule = module {
    single { DocumentFiles(androidContext()) }
    single<BackupRepository> {
        val context = androidContext()
        val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName
        DefaultBackupRepository(
            dao = get(),
            dataProvider = get(),
            appearance = get(),
            ambience = get(),
            goals = get(),
            appVersion = version.orEmpty()
        )
    }
    // A mano: el constructor tiene parámetros opcionales para los tests que Koin no sabría resolver.
    single<SessionRepository> { DefaultSessionRepository(get(), get()) }
    single { GoalRepository(get(), get()) }
    // Crearlo configura RevenueCat; App lo pide al arrancar. Mientras ProFeatures esté apagado, no
    // hay límites aunque no se tenga Pro.
    single<PurchasesRepository> {
        DefaultPurchasesRepository(REVENUECAT_API_KEY?.let { RevenueCatStore(androidContext(), it) }, get())
    }
    single { ProGate(get<PurchasesRepository>().isPro) }
    single<TagRepository> { DefaultTagRepository(get(), get(), get()) }
    single<TaskRepository> { DefaultTaskRepository(get(), get(), get()) }
    single<StatsRepository> { DefaultStatsRepository(get()) }
    single<TimeSource> { SystemTimeSource(androidContext()) }
    singleOf(::FocusEngine)
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
