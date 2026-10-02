package com.myday.litu.core.domain.usecase

import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.scheduling.SpacedRepetition
import com.myday.litu.core.model.Attempt
import com.myday.litu.core.model.Question
import com.myday.litu.core.model.StudyMode
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Applies the spaced-repetition rules after an answer (spec section 9.1). */
class ReviewUpdater @Inject constructor(
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    suspend fun onAnswered(questionId: String, correct: Boolean, now: Instant) {
        val existing = progress.reviewState(questionId)
        // No row is created for a question answered correctly the first time.
        if (existing == null && correct) return
        val daysUntilTest = SpacedRepetition.daysUntilTest(settings.current().testDate, now, clock.zone)
        val base = existing ?: SpacedRepetition.newState(questionId, now)
        progress.upsertReviewState(SpacedRepetition.schedule(base, correct, now, daysUntilTest))
    }
}

/** Records one practice, review, sample or timer answer and updates review state and today's stats. */
class RecordAnswerUseCase @Inject constructor(
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val reviewUpdater: ReviewUpdater,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        question: Question,
        selected: List<String>,
        mode: StudyMode,
        sessionId: String,
        timeMs: Long,
    ): Boolean {
        val now = clock.instant()
        val correct = question.isCorrect(selected)
        progress.recordAttempt(Attempt(question.id, selected, correct, mode, sessionId, now, timeMs))
        reviewUpdater.onAnswered(question.id, correct, now)
        progress.addToDailyStat(
            day = LocalDate.now(clock),
            answered = 1,
            correct = if (correct) 1 else 0,
            studyMinutes = 0,
            dailyGoal = settings.current().dailyGoal,
        )
        return correct
    }
}

class ToggleFlagUseCase @Inject constructor(
    private val progress: ProgressRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(questionId: String, flagged: Boolean) {
        val now = clock.instant()
        val existing = progress.reviewState(questionId)
        when {
            existing != null -> progress.upsertReviewState(existing.copy(flagged = flagged, updatedAt = now))
            // Flagging creates the review row; it becomes due straight away.
            flagged -> progress.upsertReviewState(SpacedRepetition.newState(questionId, now, flagged = true))
        }
    }

    suspend fun isFlagged(questionId: String): Boolean = progress.reviewState(questionId)?.flagged == true
}

class FinishSessionUseCase @Inject constructor(
    private val progress: ProgressRepository,
    private val backupScheduler: com.myday.litu.core.domain.repository.BackupScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke(sessionId: String, answered: Int, correct: Int) {
        progress.finishSession(sessionId, answered, correct, clock.instant())
        backupScheduler.requestBackup()
    }
}
