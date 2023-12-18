package com.baltajmn.flowtime.di

import com.baltajmn.flowtime.MainViewModel
import com.baltajmn.flowtime.core.common.dispatchers.DispatchersModule
import com.baltajmn.flowtime.core.design.module.DesignModule
import com.baltajmn.flowtime.core.persistence.di.PersistenceModule
import com.baltajmn.flowtime.features.screens.di.ScreensModule
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
        includes(
            listOf(
                DispatchersModule,
                PersistenceModule,
                DesignModule
            )
        )
    }