package com.baltajmn.flowtime.data.timer

import android.content.Context
import android.os.SystemClock
import android.provider.Settings

class SystemTimeSource(private val context: Context) : TimeSource {
    override fun wallMillis() = System.currentTimeMillis()

    override fun elapsedMillis() = SystemClock.elapsedRealtime()

    override fun bootCount() =
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
}
