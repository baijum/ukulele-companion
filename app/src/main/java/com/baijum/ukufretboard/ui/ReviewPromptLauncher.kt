package com.baijum.ukufretboard.ui

import android.content.Context
import android.util.Log
import com.baijum.ukufretboard.data.ReviewPromptRepository
import com.google.android.play.core.review.ReviewManagerFactory

private const val TAG = "ReviewPrompt"

/**
 * Launches the Google Play In-App Review flow.
 *
 * Play's design guidelines forbid preceding the flow with an opinion question
 * ("Enjoying the app?") or triggering it from a button, so this is called
 * directly once [ReviewPromptRepository.isEligible] passes — there is no app UI
 * in front of it. Play itself decides whether to actually show the review card.
 *
 * The attempt is recorded up front so a flow that never launches still burns the
 * cooldown, while [ReviewPromptRepository.recordReviewed] is only recorded once
 * the flow completes. Recording it earlier would permanently exclude anyone
 * whose request failed — sideloaded builds, no Play Store, quota exhausted.
 */
fun launchReviewFlow(
    context: Context,
    repository: ReviewPromptRepository,
) {
    val activity = context.findActivity()
    if (activity == null) {
        Log.d(TAG, "Review flow skipped: no host Activity")
        return
    }

    repository.recordPromptShown()

    val reviewManager = ReviewManagerFactory.create(context)
    reviewManager.requestReviewFlow().addOnCompleteListener { request ->
        if (!request.isSuccessful) {
            Log.d(TAG, "Review flow unavailable: ${request.exception?.message}")
            return@addOnCompleteListener
        }
        reviewManager
            .launchReviewFlow(activity, request.result)
            .addOnCompleteListener { repository.recordReviewed() }
    }
}
