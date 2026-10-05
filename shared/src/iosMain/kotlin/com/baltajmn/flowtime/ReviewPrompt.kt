package com.baltajmn.flowtime

import com.baltajmn.flowtime.data.review.ReviewPolicy
import platform.StoreKit.SKStoreReviewController
import platform.UIKit.UIApplication
import platform.UIKit.UIWindowScene

/**
 * La valoración del sistema, sin preguntar antes, a quien le toca según [ReviewPolicy]: como en Play,
 * es iOS quien decide si la enseña.
 */
internal suspend fun askForReview(policy: ReviewPolicy) {
    if (!policy.shouldAsk()) return
    val scene = UIApplication.sharedApplication.connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene ?: return
    policy.asked()
    SKStoreReviewController.requestReviewInScene(scene)
}
