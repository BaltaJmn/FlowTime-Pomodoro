package com.baltajmn.flowtime.features.screens.di

import com.baltajmn.flowtime.features.screens.edit.EditViewModel
import com.baltajmn.flowtime.features.screens.flowtime.FlowTimeViewModel
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
import com.baltajmn.flowtime.features.screens.percentage.PercentageViewModel
import com.baltajmn.flowtime.features.screens.pomodoro.PomodoroViewModel
import com.baltajmn.flowtime.features.screens.settings.SettingsViewModel
import com.baltajmn.flowtime.features.screens.splash.SplashViewModel
import com.baltajmn.flowtime.features.screens.todoList.TodoListViewModel
import com.baltajmn.flowtime.features.screens.todoList.domain.GetTodoListByDate
import com.baltajmn.flowtime.features.screens.todoList.domain.GetTodoListByDateUseCase
import com.baltajmn.flowtime.features.screens.todoList.domain.InsertTodoList
import com.baltajmn.flowtime.features.screens.todoList.domain.InsertTodoListUseCase
import com.baltajmn.flowtime.features.screens.todoList.domain.UpdateTodoList
import com.baltajmn.flowtime.features.screens.todoList.domain.UpdateTodoListUseCase
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
        factoryOf(::GetTodoListByDate) bind GetTodoListByDateUseCase::class
        factoryOf(::InsertTodoList) bind InsertTodoListUseCase::class
        factoryOf(::UpdateTodoList) bind UpdateTodoListUseCase::class
    }

private val ScreensPresentationModule: Module
    get() = module {
        viewModelOf(::FlowTimeViewModel)
        viewModelOf(::PomodoroViewModel)
        viewModelOf(::PercentageViewModel)
        viewModelOf(::EditViewModel)
        viewModelOf(::SettingsViewModel)
        viewModelOf(::TodoListViewModel)
        viewModelOf(::HistoryViewModel)
        viewModelOf(::OnBoardViewModel)
        viewModelOf(::SplashViewModel)
    }