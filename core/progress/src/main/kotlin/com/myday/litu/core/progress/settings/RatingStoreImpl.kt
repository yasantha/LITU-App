package com.myday.litu.core.progress.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.myday.litu.core.domain.repository.RatingStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingStoreImpl @Inject constructor(
    private val store: DataStore<Preferences>,
) : RatingStore {
    override suspend fun lastAsked(): Instant? = store.data.first()[KEY]?.let(Instant::ofEpochMilli)

    override suspend fun markAsked(at: Instant) {
        store.edit { it[KEY] = at.toEpochMilli() }
    }

    private companion object {
        val KEY = longPreferencesKey("rating_last_asked")
    }
}
