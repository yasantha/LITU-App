package com.myday.litu.core.progress.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.myday.litu.core.domain.plan.Improvement
import com.myday.litu.core.domain.plan.SavedPlan
import com.myday.litu.core.domain.plan.StudyPlanStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Today's study plan, kept in the settings DataStore (cleared with the rest on data deletion). */
@Singleton
class StudyPlanStoreImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : StudyPlanStore {

    @Serializable
    private data class Stored(
        val day: String,
        val sections: List<String>,
        val baseline: Map<String, Int>,
        val reviewTarget: Int,
        val notesRead: Set<String> = emptySet(),
        val improvements: List<List<String>> = emptyList(),
        val exploring: Boolean = false,
    )

    override suspend fun load(): SavedPlan? {
        val raw = store.data.first()[KEY] ?: return null
        val s = runCatching { json.decodeFromString<Stored>(raw) }.getOrNull() ?: return null
        return SavedPlan(
            day = LocalDate.parse(s.day),
            sectionIds = s.sections,
            baselinePercent = s.baseline,
            reviewTarget = s.reviewTarget,
            notesRead = s.notesRead,
            improvements = s.improvements.map { Improvement(it[0], it[1].toInt(), it[2].toInt()) },
            exploring = s.exploring,
        )
    }

    override suspend fun save(plan: SavedPlan) {
        val stored = Stored(
            day = plan.day.toString(),
            sections = plan.sectionIds,
            baseline = plan.baselinePercent,
            reviewTarget = plan.reviewTarget,
            notesRead = plan.notesRead,
            improvements = plan.improvements.map { listOf(it.sectionId, "${it.fromPercent}", "${it.toPercent}") },
            exploring = plan.exploring,
        )
        store.edit { it[KEY] = json.encodeToString(stored) }
    }

    private companion object {
        val KEY = stringPreferencesKey("study_plan")
        val json = Json { ignoreUnknownKeys = true }
    }
}
