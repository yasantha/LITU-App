package com.myday.litu.core.analytics

/**
 * Funnel events from spec section 12.6. Parameters never include names, emails or free text.
 */
sealed class AnalyticsEvent(val name: String, val params: Map<String, Any> = emptyMap()) {
    class OnboardingComplete(hasTestDate: Boolean, dailyGoal: Int) :
        AnalyticsEvent("onboarding_complete", mapOf("has_test_date" to hasTestDate, "daily_goal" to dailyGoal))

    class SampleQuizComplete(score: Int) : AnalyticsEvent("sample_quiz_complete", mapOf("score" to score))

    class PaywallView(source: PaywallSource, variant: String) :
        AnalyticsEvent("paywall_view", mapOf("source" to source.value, "variant" to variant))

    class TrialStart(productId: String) : AnalyticsEvent("trial_start", mapOf("product_id" to productId))

    class Purchase(productId: String) : AnalyticsEvent("purchase", mapOf("product_id" to productId))

    class PracticeSessionComplete(mode: String, answered: Int, correct: Int, durationS: Long) : AnalyticsEvent(
        "practice_session_complete",
        mapOf("mode" to mode, "answered" to answered, "correct" to correct, "duration_s" to durationS),
    )

    class MockComplete(score: Int, passed: Boolean, durationS: Long) :
        AnalyticsEvent("mock_complete", mapOf("score" to score, "passed" to passed, "duration_s" to durationS))

    class ReviewSessionComplete(answered: Int, correct: Int) :
        AnalyticsEvent("review_session_complete", mapOf("answered" to answered, "correct" to correct))

    class QuestionReported(questionId: String, reason: String) :
        AnalyticsEvent("question_reported", mapOf("question_id" to questionId, "reason" to reason))

    data object BackupLinked : AnalyticsEvent("backup_linked")
}

enum class PaywallSource(val value: String) { ONBOARDING("onboarding"), LOCKED_ITEM("locked_item"), SETTINGS("settings") }

fun interface AnalyticsLogger {
    fun log(event: AnalyticsEvent)
}
