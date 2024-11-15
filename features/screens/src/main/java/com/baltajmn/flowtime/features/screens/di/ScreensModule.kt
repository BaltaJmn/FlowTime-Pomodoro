package com.baltajmn.flowtime.features.screens.di

import com.baltajmn.flowtime.features.screens.flowtime.FlowTimeViewModel
import com.baltajmn.flowtime.features.screens.history.HistoryViewModel
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.percentage.PercentageViewModel
import com.baltajmn.flowtime.features.screens.pomodoro.PomodoroViewModel
import com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val ScreensModule = module {
    includes(
        ScreensDataModule,
        ScreensDomainModule,
        ScreensPresentationModule
    )
}

private val ScreensDataModule: Module
    get() = module {}

private val ScreensDomainModule: Module
    get() = module {
        factoryOf(::GetStudyTime) bind GetStudyTimeUseCase::class
    }

private val ScreensPresentationModule: Module
    get() = module {
        viewModelOf(::FlowTimeViewModel)
        viewModelOf(::PomodoroViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::PercentageViewModel)
        viewModelOf(::HistoryViewModel)
    }