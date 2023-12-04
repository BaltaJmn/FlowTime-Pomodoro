package com.baltajmn.flowtime.di

import com.baltajmn.flowtime.core.persistence.di.PersistenceModule
import org.koin.core.module.Module
import org.koin.dsl.module

val FeaturesModule: Module
    get() = module {
        includes(
            listOf(

            )
        )
    }

val CoreModules: Module
    get() = module {
        includes(
            listOf(
                PersistenceModule,
            )
        )
    }