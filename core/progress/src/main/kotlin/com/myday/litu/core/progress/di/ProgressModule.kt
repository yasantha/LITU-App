package com.myday.litu.core.progress.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.myday.litu.core.domain.repository.ProgressRepository
import com.myday.litu.core.domain.repository.SettingsRepository
import com.myday.litu.core.progress.ProgressRepositoryImpl
import com.myday.litu.core.progress.db.ProgressDatabase
import com.myday.litu.core.progress.settings.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ProgressModule {
    @Binds
    abstract fun bindProgressRepository(impl: ProgressRepositoryImpl): ProgressRepository

    @Binds
    abstract fun bindStudyPlanStore(impl: com.myday.litu.core.progress.settings.StudyPlanStoreImpl): com.myday.litu.core.domain.plan.StudyPlanStore

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    companion object {
        @Provides
        @Singleton
        @Suppress("SpreadOperator") // Runs once at startup.
        fun provideProgressDatabase(@ApplicationContext context: Context): ProgressDatabase =
            Room.databaseBuilder(context, ProgressDatabase::class.java, ProgressDatabase.NAME)
                .addMigrations(*ProgressDatabase.MIGRATIONS)
                .build()

        @Provides
        @Singleton
        fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
            PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
    }
}
