package com.baltajmn.flowtime.data.timer

import kotlin.time.Clock
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime_nsec_np
import platform.posix.size_tVar
import platform.darwin.sysctlbyname
import platform.posix.timeval

@OptIn(ExperimentalForeignApi::class)
class IosTimeSource : TimeSource {
    override fun wallMillis() = Clock.System.now().toEpochMilliseconds()

    // CLOCK_MONOTONIC, en iOS, sigue contando con el móvil bloqueado y dormido: como elapsedRealtime.
    override fun elapsedMillis() = (clock_gettime_nsec_np(CLOCK_MONOTONIC.convert()) / 1_000_000u).toLong()

    // iOS no cuenta los arranques: el segundo en que arrancó el móvil cambia en cada reinicio, que es
    // lo que importa.
    override fun bootCount(): Int = memScoped {
        val boot = alloc<timeval>()
        val size = alloc<size_tVar>().apply { value = sizeOf<timeval>().convert() }
        sysctlbyname("kern.boottime", boot.ptr, size.ptr, null, 0u)
        boot.tv_sec.toInt()
    }
}
