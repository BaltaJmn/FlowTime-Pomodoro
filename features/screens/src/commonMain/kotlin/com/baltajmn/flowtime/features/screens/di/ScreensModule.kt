package com.baltajmn.flowtime.features.screens.di

import com.baltajmn.flowtime.data.timer.FocusEngine
import com.baltajmn.flowtime.features.screens.edit.EditViewModel
import com.baltajmn.flowtime.features.screens.focus.FocusViewModel
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboard
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboardUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboard
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboardUseCase
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel
import com.baltajmn.flowtime.features.screens.pro.ProLauncher
import com.baltajmn.flowtime.features.screens.pro.ProViewModel
import com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
import com.baltajmn.flowtime.features.screens.splash.SplashViewModel
import com.baltajmn.flowtime.features.screens.stats.StatsViewModel
import com.baltajmn.flowtime.features.screens.support.SupportViewModel
import com.baltajmn.flowtime.features.screens.todoList.TodoListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
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
        factoryOf(::GetAllStudyTime) bind GetAllStudyTimeUseCase::class
        factoryOf(::SetStudyTimeFromClipboard) bind SetStudyTimeFromClipboardUseCase::class
        factoryOf(::GetStudyTimeToClipboard) bind GetStudyTimeToClipboardUseCase::class
    }

private val ScreensPresentationModule: Module
    get() = module {
        viewModelOf(::FocusViewModel)
        viewModel { params -> EditViewModel(params.get(), get()) }
        viewModelOf(::SettingsViewModel)
        // A mano: el constructor tiene un parámetro opcional para los tests que Koin no sabría resolver.
        viewModel { TodoListViewModel(get()) }
        viewModel {
            val engine = get<FocusEngine>()
            StatsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), { engine.state.value.isActive })
        }
        viewModelOf(::OnBoardViewModel)
        viewModelOf(::SplashViewModel)
        viewModelOf(::SupportViewModel)
        viewModelOf(::ProViewModel)
        single { ProLauncher() }
    }