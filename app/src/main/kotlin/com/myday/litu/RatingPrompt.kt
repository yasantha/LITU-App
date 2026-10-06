package com.myday.litu

import android.app.Activity
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.myday.litu.core.domain.usecase.GoodMoment
import com.myday.litu.core.domain.usecase.RatingMomentUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play in-app review at good moments only (a passed mock, a 7-day streak). It works only for
 * installs from Play; elsewhere Play shows nothing. Google forbids asking "Do you like the app?"
 * first, so the prompt is shown as is.
 */
@Singleton
class RatingPrompt @Inject constructor(
    private val moments: RatingMomentUseCase,
) {
    suspend fun maybeAsk(activity: Activity, moment: GoodMoment) {
        if (!moments.shouldAsk()) return
        Log.d("RatingPrompt", "Asking for a rating after $moment")
        runCatching {
            val manager = ReviewManagerFactory.create(activity)
            val info = manager.requestReview()
            moments.markAsked()
            manager.launchReview(activity, info)
        }.onFailure { Log.w("RatingPrompt", "In-app review unavailable", it) }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RatingEntryPoint {
    fun ratingPrompt(): RatingPrompt
}
