package com.baijum.ukufretboard.ui

import android.app.Activity
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.baijum.ukufretboard.R
import com.baijum.ukufretboard.data.ReviewPromptRepository
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Two-step review prompt dialog.
 *
 * Step 1: "You just earned [achievement]! Enjoying Ukulele Companion?"
 *   - Yes -> launches the Google Play In-App Review flow
 *   - No  -> shows a brief thank-you message, then dismisses
 *
 * @param achievementTitle The title of the achievement that was just unlocked.
 * @param repository Repository for recording prompt outcomes.
 * @param onDismiss Called when the dialog is fully dismissed.
 */
@Composable
fun ReviewPromptDialog(
    achievementTitle: String,
    repository: ReviewPromptRepository,
    onDismiss: () -> Unit,
) {
    var showFeedback by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (showFeedback) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                Text(stringResource(R.string.review_feedback_thanks))
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.ok))
                }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = {
                repository.recordDismissal()
                onDismiss()
            },
            title = {
                Text(
                    text = stringResource(R.string.review_prompt_title),
                    modifier = Modifier.semantics { heading() },
                )
            },
            text = {
                Text(stringResource(R.string.review_prompt_message, achievementTitle))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repository.recordReviewed()
                        val activity = context as? Activity
                        if (activity != null) {
                            val reviewManager = ReviewManagerFactory.create(context)
                            reviewManager.requestReviewFlow().addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    reviewManager.launchReviewFlow(activity, task.result)
                                } else {
                                    android.util.Log.d(
                                        "ReviewPrompt",
                                        "Review flow unavailable: ${task.exception?.message}",
                                    )
                                }
                            }
                        }
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.review_prompt_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        repository.recordDismissal()
                        showFeedback = true
                    },
                ) {
                    Text(stringResource(R.string.review_prompt_no))
                }
            },
        )
    }
}
