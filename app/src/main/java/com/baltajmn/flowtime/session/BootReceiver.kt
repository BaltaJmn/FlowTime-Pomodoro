package com.baltajmn.flowtime.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Al reiniciar el móvil solo se encarga el trabajo; lo hace SessionReceiver desde BootWorker. Desde
 * Android 15 un receptor de BOOT_COMPLETED no puede arrancar un servicio de reproducción, y Play lo
 * busca en todo lo que alcanza este onReceive: así no llega a AmbientService ni por la vía de Koin.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        WorkManager.getInstance(context).enqueue(OneTimeWorkRequest.from(BootWorker::class.java))
    }
}

/** Pone al día el motor, la notificación y las alarmas, que el sistema borra al reiniciar. */
class BootWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        applicationContext.sendBroadcast(Intent(applicationContext, SessionReceiver::class.java))
        return Result.success()
    }
}
