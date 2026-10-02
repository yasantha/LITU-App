package com.myday.litu.core.domain.usecase

import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.streak.StreakInfo
import com.myday.litu.core.domain.streak.Streaks
import com.myday.litu.core.model.DailyStat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class TodayProgress(
    val answered: Int,
    val goal: Int,
    val streak: StreakInfo,
    val stats: List<DailyStat>,
) {
    val remaining: Int get() = (goal - answered).coerceAtLeast(0)
    val fraction: Float get() = if (goal == 0) 0f else (answered.toFloat() / goal).coerceIn(0f, 1f)
}

class ObserveTodayUseCase @Inject constructor(
    private val progress: ProgressRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<TodayProgress> =
        combine(progress.observeDailyStats(), settings.settings) { stats, s ->
            val today = LocalDate.now(clock)
            TodayProgress(
                answered = stats.firstOrNull { it.day == today }?.answered ?: 0,
                goal = s.dailyGoal,
                streak = Streaks.compute(stats.filter { it.goalMet }.mapTo(HashSet()) { it.day }, today),
                stats = stats,
            )
        }
}
