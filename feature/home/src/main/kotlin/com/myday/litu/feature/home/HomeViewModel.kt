package com.myday.litu.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.domain.plan.StudyPlan
import com.myday.litu.core.domain.plan.StudyPlanUseCase
import com.myday.litu.core.domain.readiness.ReadinessLevel
import com.myday.litu.core.domain.repository.ConfigRepository
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.domain.scheduling.SpacedRepetition
import com.myday.litu.core.domain.usecase.ObserveTodayUseCase
import com.myday.litu.core.domain.usecase.ProgressOverviewUseCase
import com.myday.litu.core.domain.usecase.TodayProgress
import com.myday.litu.core.domain.streak.StreakInfo
import com.myday.litu.core.model.Exam
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class WeakTopic(val sectionId: String, val title: String, val chapterNumber: Int)

data class HomeState(
    val today: LocalDate,
    val readiness: Int? = null,
    val level: ReadinessLevel? = null,
    val lastMockScores: List<Int> = emptyList(),
    val daysUntilTest: Int? = null,
    val todayProgress: TodayProgress = TodayProgress(0, 20, StreakInfo.EMPTY, emptyList()),
    val dueCount: Int = 0,
    val weakTopics: List<WeakTopic> = emptyList(),
    val weakestChapterIds: List<String> = emptyList(),
    val banner: String = "",
    val isPro: Boolean = false,
    val plan: StudyPlan? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val overview: ProgressOverviewUseCase,
    private val progress: ProgressRepository,
    private val studyPlan: StudyPlanUseCase,
    observeToday: ObserveTodayUseCase,
    settings: SettingsRepository,
    config: ConfigRepository,
    entitlements: EntitlementRepository,
    private val clock: Clock,
) : ViewModel() {
    private val loaded = MutableStateFlow(HomeState(LocalDate.now(clock)))

    val state: StateFlow<HomeState> = combine(
        loaded, observeToday(), settings.settings, config.config, entitlements.isPro,
    ) { base, today, s, cfg, pro ->
        base.copy(
            today = LocalDate.now(clock),
            todayProgress = today,
            daysUntilTest = s.testDate?.let { SpacedRepetition.daysUntilTest(it, clock.instant(), clock.zone) },
            banner = cfg.bannerMessage,
            isPro = pro,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), loaded.value)

    fun refresh() = viewModelScope.launch {
        val o = overview()
        val due = progress.dueReviews(clock.instant(), Exam.REVIEW_SESSION_CAP).size
        loaded.value = loaded.value.copy(
            readiness = o.readiness,
            level = o.readinessLevel,
            lastMockScores = o.recentMocks.take(3).mapNotNull { it.score }.reversed(),
            dueCount = due,
            weakTopics = o.weakestSections.mapNotNull { st ->
                val section = o.section(st.sectionId) ?: return@mapNotNull null
                val chapter = o.chapter(st.chapterId) ?: return@mapNotNull null
                WeakTopic(section.id, section.title, chapter.number)
            },
            weakestChapterIds = o.weakestChapters(2).map { it.id },
            plan = studyPlan(),
        )
    }
}
