package com.myday.litu.core.progress.db

import com.myday.litu.core.model.Attempt
import com.myday.litu.core.model.ChapterScore
import com.myday.litu.core.model.DailyStat
import com.myday.litu.core.model.MockExam
import com.myday.litu.core.model.ReviewState
import com.myday.litu.core.model.StudyMode
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

internal val progressJson = Json { ignoreUnknownKeys = true }

internal fun AttemptEntity.toModel() = Attempt(
    questionId = questionId,
    selected = progressJson.decodeFromString(selected),
    correct = correct == 1,
    mode = StudyMode.fromDb(mode),
    sessionId = sessionId,
    answeredAt = Instant.ofEpochMilli(answeredAt),
    timeMs = timeMs,
)

internal fun Attempt.toEntity() = AttemptEntity(
    questionId = questionId,
    selected = progressJson.encodeToString(selected),
    correct = if (correct) 1 else 0,
    mode = mode.dbValue,
    sessionId = sessionId,
    answeredAt = answeredAt.toEpochMilli(),
    timeMs = timeMs,
)

internal fun ReviewStateEntity.toModel() = ReviewState(
    questionId = questionId,
    ease = ease,
    intervalDays = intervalDays,
    dueAt = Instant.ofEpochMilli(dueAt),
    reps = reps,
    lapses = lapses,
    flagged = flagged == 1,
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

internal fun ReviewState.toEntity() = ReviewStateEntity(
    questionId = questionId,
    ease = ease,
    intervalDays = intervalDays,
    dueAt = dueAt.toEpochMilli(),
    reps = reps,
    lapses = lapses,
    flagged = if (flagged) 1 else 0,
    updatedAt = updatedAt.toEpochMilli(),
)

internal fun MockExamEntity.toModel() = MockExam(
    id = id,
    startedAt = Instant.ofEpochMilli(startedAt),
    finishedAt = finishedAt?.let(Instant::ofEpochMilli),
    questionIds = progressJson.decodeFromString(questionIds),
    answers = progressJson.decodeFromString(answers),
    score = score,
    passed = passed?.let { it == 1 },
    chapterBreakdown = chapterBreakdown
        ?.let { progressJson.decodeFromString<Map<String, List<Int>>>(it) }
        ?.mapValues { (_, v) -> ChapterScore(v[0], v[1]) }
        .orEmpty(),
    contentVersion = contentVersion,
)

internal fun MockExam.toEntity() = MockExamEntity(
    id = id,
    startedAt = startedAt.toEpochMilli(),
    finishedAt = finishedAt?.toEpochMilli(),
    questionIds = progressJson.encodeToString(questionIds),
    answers = progressJson.encodeToString(answers),
    score = score,
    passed = passed?.let { if (it) 1 else 0 },
    chapterBreakdown = progressJson.encodeToString(chapterBreakdown.mapValues { listOf(it.value.correct, it.value.total) }),
    contentVersion = contentVersion,
)

internal fun DailyStatEntity.toModel() = DailyStat(LocalDate.parse(day), answered, correct, studyMinutes, goalMet == 1)

internal fun DailyStat.toEntity() =
    DailyStatEntity(day.toString(), answered, correct, studyMinutes, if (goalMet) 1 else 0)
