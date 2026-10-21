package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.backup.BackupRepository
import com.baltajmn.flowtime.data.backup.DefaultBackupRepository
import com.baltajmn.flowtime.data.goal.GoalRepository
import com.baltajmn.flowtime.data.pro.ProGate
import com.baltajmn.flowtime.data.pro.ProOfferPolicy
import com.baltajmn.flowtime.data.pro.PurchasesRepository
import com.baltajmn.flowtime.data.reminder.ReminderRepository
import com.baltajmn.flowtime.data.repository.DefaultSessionRepository
import com.baltajmn.flowtime.data.repository.SessionRepository
import com.baltajmn.flowtime.data.stats.DefaultStatsRepository
import com.baltajmn.flowtime.data.stats.StatsRepository
import com.baltajmn.flowtime.data.tag.DefaultTagRepository
import com.baltajmn.flowtime.data.tag.TagRepository
import com.baltajmn.flowtime.data.task.DefaultTaskRepository
import com.baltajmn.flowtime.data.task.TaskRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.coop.CoopAccount
import com.baltajmn.flowtime.data.coop.CoopFriends
import com.baltajmn.flowtime.data.coop.CoopRooms
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

/** La versión de la app, como la enseña la tienda: la pone el DataModule de cada plataforma. */
internal val APP_VERSION = named("appVersion")

/**
 * Lo que es igual en Android y en el iPhone. El DataModule de cada plataforma pone el resto: la
 * versión ([APP_VERSION]), los ficheros, la tienda y el reloj.
 */
internal val RepositoriesModule = module {
    single<BackupRepository> {
        DefaultBackupRepository(
            dao = get(),
            dataProvider = get(),
            appearance = get(),
            ambience = get(),
            goals = get(),
            reminders = get(),
            mixes = get(),
            appVersion = get(APP_VERSION)
        )
    }
    // A mano: el constructor tiene parámetros opcionales para los tests que Koin no sabría resolver.
    single<SessionRepository> { DefaultSessionRepository(get(), get()) }
    single { GoalRepository(get(), get()) }
    single { ReminderRepository(get()) }
    // Mientras ProFeatures esté apagado, no hay límites aunque no se tenga Pro.
    single { ProGate(get<PurchasesRepository>().isPro) }
    single {
        val engine = get<FocusEngine>()
        val purchases = get<PurchasesRepository>()
        ProOfferPolicy(
            dataProvider = get(),
            sessions = get(),
            isPro = { purchases.isPro.value },
            proEnded = { purchases.proEnded.value },
            sessionRunning = { engine.state.value.isActive }
        )
    }
    single<TagRepository> { DefaultTagRepository(get(), get(), get()) }
    single<TaskRepository> { DefaultTaskRepository(get(), get(), get()) }
    single<StatsRepository> { DefaultStatsRepository(get()) }
    singleOf(::FocusEngine)
    singleOf(::CoopAccount)
    singleOf(::CoopFriends)
    // Al arrancar: con una sala a medias, los botones del temporizador ya tienen que ir a la sala.
    single(createdAtStart = true) { CoopRooms(get(), get(), get(), get()) }
}
