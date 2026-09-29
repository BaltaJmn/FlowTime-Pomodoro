package com.baltajmn.flowtime.review

import android.app.Activity
import android.util.Log
import com.baltajmn.flowtime.data.review.ReviewPolicy
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlin.coroutines.cancellation.CancellationException

/**
 * El diálogo de valoración de Play, sin preguntar antes: Play decide si lo enseña. Solo se pide en
 * los momentos tranquilos que busca MainActivity, y a quien le toca según [ReviewPolicy].
 */
class ReviewPrompter(private val policy: ReviewPolicy) {

    suspend fun askIfDue(activity: Activity) {
        if (!policy.shouldAsk()) return
        val manager = ReviewManagerFactory.create(activity)
        try {
            val info = manager.requestReview()
            // Antes de lanzarlo: el diálogo pausa la actividad, y con ella esta corrutina.
            policy.asked()
            manager.launchReview(activity, info)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin Play Store, o Play no puede ahora: se volverá a probar al terminar otra sesión.
            Log.w("ReviewPrompter", "No se ha podido pedir la valoración", e)
        }
    }
}
