package com.baltajmn.flowtime.core.common.dispatchers

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val io: CoroutineDispatcher
    val mainImmediate: CoroutineDispatcher
    val default: CoroutineDispatcher
}

class DefaultDispatcherProvider : DispatcherProvider {
    override val io = Dispatchers.IO
    override val mainImmediate = Dispatchers.Main.immediate
    override val default = Dispatchers.Default
}

class TestDispatcherProvider : DispatcherProvider {
    override val io = Dispatchers.Unconfined
    override val mainImmediate = Dispatchers.Unconfined
    override val default = Dispatchers.Unconfined
}