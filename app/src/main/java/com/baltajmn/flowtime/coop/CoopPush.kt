package com.baltajmn.flowtime.coop

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.baltajmn.flowtime.core.design.R
import com.baltajmn.flowtime.data.coop.CoopAccount
import com.baltajmn.flowtime.data.coop.PushToken
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import org.koin.android.ext.android.get
import java.util.Locale

/**
 * Avisos de Coop con la app cerrada (docs/coop-push.md): el token de FCM va a [CoopAccount.pushToken].
 * El aviso lo pinta el sistema; con la app delante no se enseña, porque Realtime ya pone la lista al día.
 */
object CoopPush {

    /** El de `channel_id` en flowtime-push. */
    const val CHANNEL = "coop"

    // La mitad pública del proyecto de Firebase flowtime-pomodoro, la de google-services.json: sin el
    // plugin de Gradle.
    private val options = FirebaseOptions.Builder()
        .setProjectId("flowtime-pomodoro")
        .setApplicationId("1:891606962323:android:71a5a5e4134545c0047d66")
        .setApiKey("AIzaSyBD5f4sh5S-xFz8yQ-ubDzXPhtzO-U6g3E")
        .setGcmSenderId("891606962323")
        .build()

    fun start(context: Context, account: CoopAccount) {
        if (!account.available) return
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.coop_title))
                .build()
        )
        if (FirebaseApp.getApps(context).isEmpty()) FirebaseApp.initializeApp(context, options)
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { publish(account, it) }
            .addOnFailureListener { Log.w("CoopPush", "sin token de FCM", it) }
    }

    fun publish(account: CoopAccount, token: String) {
        // Java aún llama "in" al indonesio.
        val lang = Locale.getDefault().language.let { if (it == "in") "id" else it }
        account.pushToken.value = PushToken(token, "android", lang)
    }
}

class CoopPushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) = CoopPush.publish(get(), token)
}
