package com.baltajmn.flowtime.data.di

import com.baltajmn.flowtime.data.repository.DefaultTodoListRepository
import com.baltajmn.flowtime.data.repository.TodoListRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val DataModule = module {
    singleOf(::DefaultTodoListRepository) bind TodoListRepository::class
}
