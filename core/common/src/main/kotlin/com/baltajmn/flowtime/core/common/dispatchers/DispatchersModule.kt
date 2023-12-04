package com.baltajmn.flowtime.core.common.dispatchers

import org.koin.core.module.Module
import org.koin.dsl.module

val DispatchersModule: Module
    get() = module {
        single<DispatcherProvider> { DefaultDispatcherProvider() }
    }