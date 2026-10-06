package com.myday.litu.core.domain.usecase

import com.myday.litu.core.domain.repository.RatingStore
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/** Good moments to ask for a Play rating: never mid-study, only after something went well. */
enum class GoodMoment { PASSED_MOCK, WEEK_STREAK }

/**
 * Decides whether to show the Play in-app review prompt. At most once every [MIN_GAP]; Google also
 * applies its own limit and never reveals whether the user rated.
 */
class RatingMomentUseCase @Inject constructor(
    private val store: RatingStore,
    private val clock: Clock,
) {
    suspend fun shouldAsk(): Boolean {
        val last = store.lastAsked() ?: return true
        return Duration.between(last, clock.instant()) >= MIN_GAP
    }

    suspend fun markAsked() = store.markAsked(clock.instant())

    companion object {
        val MIN_GAP: Duration = Duration.ofDays(120)
        const val STREAK_DAYS = 7
    }
}
