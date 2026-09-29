package com.baltajmn.flowtime.features.screens.di

import com.baltajmn.flowtime.features.screens.edit.EditViewModel
import com.baltajmn.flowtime.features.screens.history.HistoryViewModel
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetAllStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTime
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboard
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeToClipboardUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.GetStudyTimeUseCase
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboard
import com.baltajmn.flowtime.features.screens.history.usecases.SetStudyTimeFromClipboardUseCase
import com.baltajmn.flowtime.features.screens.onboard.OnBoardViewModel
import com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
import com.baltajmn.flowtime.features.screens.splash.SplashViewModel
import com.baltajmn.flowtime.features.screens.timer.TimerViewModel
import com.baltajmn.flowtime.features.screens.todoList.TodoListViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
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
        factoryOf(::GetAllStudyTime) bind GetAllStudyTimeUseCase::class
        factoryOf(::SetStudyTimeFromClipboard) bind SetStudyTimeFromClipboardUseCase::class
        factoryOf(::GetStudyTimeToClipboard) bind GetStudyTimeToClipboardUseCase::class
    }

private val ScreensPresentationModule: Module
    get() = module {
        viewModel { params -> TimerViewModel(params.get(), get(), get(), get(), get()) }
        viewModelOf(::EditViewModel)
        viewModelOf(::SettingsViewModel)
        // A mano: el constructor tiene un parámetro opcional para los tests que Koin no sabría resolver.
        viewModel { TodoListViewModel(get()) }
        viewModelOf(::HistoryViewModel)
        viewModelOf(::OnBoardViewModel)
        viewModelOf(::SplashViewModel)
    }