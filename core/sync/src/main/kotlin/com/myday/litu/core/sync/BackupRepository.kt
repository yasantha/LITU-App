package com.myday.litu.core.sync

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.myday.litu.core.config.FirebaseAvailability
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.sync.BackupSettings
import com.myday.litu.core.domain.sync.BackupSnapshot
import com.myday.litu.core.domain.sync.MockSummary
import com.myday.litu.core.domain.sync.StreakSummary
import com.myday.litu.core.model.ChapterScore
import com.myday.litu.core.model.DailyStat
import com.myday.litu.core.model.ReviewState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * One Firestore document per user, users/{uid} (spec section 12.3), plus create-only question
 * reports. Attempts stay on the phone; only the state needed to restore is synced.
 */
@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progress: ProgressRepository,
    private val clock: Clock,
) {
    private val available get() = FirebaseAvailability.isConfigured(context)
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private fun linkedUid(): String? = if (!available) null else auth.currentUser?.takeIf { !it.isAnonymous }?.uid

    /** Writes the backup document. Only for users with a linked Google account. */
    suspend fun backupNow(): Boolean {
        val uid = linkedUid() ?: return false
        val snapshot = progress.exportSnapshot(clock.instant())
        firestore.collection(USERS).document(uid).set(toDocument(snapshot)).await()
        progress.markSessionsSynced()
        return true
    }

    /** One read on app start (signed-in users only): merges the remote document when it is newer. */
    suspend fun restore(): Boolean {
        val uid = linkedUid() ?: return false
        val doc = firestore.collection(USERS).document(uid).get().await()
        val data = doc.data ?: return false
        val remote = fromDocument(data) ?: return false
        progress.importSnapshot(remote)
        return true
    }

    /** Sends queued reports. Any signed-in user, including anonymous ones, can report. */
    suspend fun sendReports(): Int {
        if (!available) return 0
        val uid = auth.currentUser?.uid ?: return 0
        val pending = progress.pendingReports()
        val sent = mutableListOf<Long>()
        for (r in pending) {
            runCatching {
                firestore.collection(REPORTS).add(
                    mapOf(
                        "uid" to uid,
                        "questionId" to r.questionId,
                        "reason" to r.reason.storageValue,
                        "comment" to r.comment,
                        "contentVersion" to r.contentVersion,
                        "appVersion" to r.appVersion,
                        "createdAt" to Timestamp(r.createdAt.epochSecond, 0),
                    ),
                ).await()
            }.onSuccess { sent += r.id }.onFailure { Log.w(TAG, "Report ${r.id} not sent", it) }
        }
        progress.removeReports(sent)
        return sent.size
    }

    suspend fun deleteRemote(uid: String) {
        if (available) firestore.collection(USERS).document(uid).delete().await()
    }

    private fun toDocument(s: BackupSnapshot): Map<String, Any?> = mapOf(
        "schemaVersion" to s.schemaVersion,
        "updatedAt" to FieldValue.serverTimestamp(),
        "clientUpdatedAt" to s.updatedAt.toEpochMilli(),
        "settings" to mapOf(
            "testDate" to s.settings.testDate?.toString(),
            "dailyGoal" to s.settings.dailyGoal,
            "reminderTime" to s.settings.reminderTime.toString(),
        ),
        "reviewState" to s.reviewStates.associate {
            it.questionId to listOf(
                it.ease, it.intervalDays.toLong(), it.dueAt.toEpochMilli(), it.reps.toLong(), it.lapses.toLong(),
                if (it.flagged) 1L else 0L, it.updatedAt.toEpochMilli(),
            )
        },
        "mocks" to s.mocks.map { m ->
            mapOf(
                "id" to m.id,
                "finishedAt" to m.finishedAt.toEpochMilli(),
                "score" to m.score,
                "passed" to m.passed,
                "breakdown" to m.breakdown.mapValues { listOf(it.value.correct, it.value.total) },
            )
        },
        "dailyStats" to s.dailyStats.associate { it.day.toString() to listOf(it.answered, it.correct, it.studyMinutes, if (it.goalMet) 1 else 0) },
        "streak" to mapOf("current" to s.streak.current, "best" to s.streak.best, "lastDay" to s.streak.lastDay?.toString()),
    )

    @Suppress("UNCHECKED_CAST")
    private fun fromDocument(d: Map<String, Any?>): BackupSnapshot? = runCatching {
        val settings = d["settings"] as Map<String, Any?>
        val streak = d["streak"] as? Map<String, Any?> ?: emptyMap()
        BackupSnapshot(
            schemaVersion = (d["schemaVersion"] as Number).toInt(),
            updatedAt = Instant.ofEpochMilli((d["clientUpdatedAt"] as? Number)?.toLong() ?: 0L),
            settings = BackupSettings(
                testDate = (settings["testDate"] as? String)?.let(LocalDate::parse),
                dailyGoal = (settings["dailyGoal"] as Number).toInt(),
                reminderTime = LocalTime.parse(settings["reminderTime"] as String),
            ),
            reviewStates = (d["reviewState"] as? Map<String, List<Number>>).orEmpty().map { (id, v) ->
                ReviewState(
                    questionId = id,
                    ease = v[0].toDouble(),
                    intervalDays = v[1].toInt(),
                    dueAt = Instant.ofEpochMilli(v[2].toLong()),
                    reps = v[3].toInt(),
                    lapses = v[4].toInt(),
                    flagged = v[5].toInt() == 1,
                    updatedAt = Instant.ofEpochMilli(v[6].toLong()),
                )
            },
            mocks = (d["mocks"] as? List<Map<String, Any?>>).orEmpty().map { m ->
                MockSummary(
                    id = m["id"] as String,
                    finishedAt = Instant.ofEpochMilli((m["finishedAt"] as Number).toLong()),
                    score = (m["score"] as Number).toInt(),
                    passed = m["passed"] as Boolean,
                    breakdown = (m["breakdown"] as? Map<String, List<Number>>).orEmpty()
                        .mapValues { ChapterScore(it.value[0].toInt(), it.value[1].toInt()) },
                )
            },
            dailyStats = (d["dailyStats"] as? Map<String, List<Number>>).orEmpty().map { (day, v) ->
                DailyStat(LocalDate.parse(day), v[0].toInt(), v[1].toInt(), v[2].toInt(), v.getOrNull(3)?.toInt() == 1)
            },
            streak = StreakSummary(
                current = (streak["current"] as? Number)?.toInt() ?: 0,
                best = (streak["best"] as? Number)?.toInt() ?: 0,
                lastDay = (streak["lastDay"] as? String)?.let(LocalDate::parse),
            ),
        )
    }.onFailure { Log.w(TAG, "Unreadable backup document", it) }.getOrNull()

    private companion object {
        const val TAG = "Backup"
        const val USERS = "users"
        const val REPORTS = "reports"
    }
}
