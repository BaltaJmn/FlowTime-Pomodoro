package com.baltajmn.flowtime.di

import com.baltajmn.flowtime.MainViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatchersModule
import com.baltajmn.flowtime.core.database.di.DatabaseModule
import com.baltajmn.flowtime.core.design.module.DesignModule
import com.baltajmn.flowtime.core.design.service.SoundViewModel
import com.baltajmn.flowtime.core.persistence.di.PersistenceModule
import com.baltajmn.flowtime.data.di.DataModule
import com.baltajmn.flowtime.features.screens.di.ScreensModule
import com.baltajmn.flowtime.session.PhaseAlarm
import com.baltajmn.flowtime.session.SessionNotification
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.dsl.module

val FeaturesModule: Module
    get() = module {
        includes(
            listOf(
                ScreensModule
            )
        )
    }

val CoreModules: Module
    get() = module {
        viewModelOf(::MainViewModel)
        viewModelOf(::SoundViewModel)
        single { SessionNotification(androidContext(), get()) }
        single { PhaseAlarm(androidContext(), get()) }
        includes(
            listOf(
                DispatchersModule,
                PersistenceModule,
                DataModule,
                DatabaseModule,
                DesignModule
            )
        )
    }