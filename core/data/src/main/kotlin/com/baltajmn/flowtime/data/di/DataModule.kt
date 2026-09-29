package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.repository.DefaultSessionRepository
import com.baltajmn.flowtime.data.repository.DefaultTodoListRepository
import com.baltajmn.flowtime.data.repository.SessionRepository
import com.baltajmn.flowtime.data.repository.TodoListRepository
import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.data.timer.SystemTimeSource
import com.baltajmn.flowtime.data.timer.TimeSource
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val DataModule = module {
    singleOf(::DefaultTodoListRepository) bind TodoListRepository::class
    // A mano: el constructor tiene parámetros opcionales para los tests que Koin no sabría resolver.
    single<SessionRepository> { DefaultSessionRepository(get(), get()) }
    single<TimeSource> { SystemTimeSource(androidContext()) }
    singleOf(::FocusEngine)
}
