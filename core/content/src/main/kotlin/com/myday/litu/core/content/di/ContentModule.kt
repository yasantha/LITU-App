package com.myday.litu.core.content.di

import android.content.Context
import androidx.room.Room
import com.myday.litu.core.content.ContentInstaller
import com.myday.litu.core.content.ContentRepositoryImpl
import com.myday.litu.core.content.db.ContentDao
import com.myday.litu.core.content.db.ContentDatabase
import com.myday.litu.core.domain.repository.ContentRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ContentModule {
    @Binds
    abstract fun bindContentRepository(impl: ContentRepositoryImpl): ContentRepository

    companion object {
        @Provides
        @Singleton
        fun provideContentDatabase(@ApplicationContext context: Context): ContentDatabase {
            ContentInstaller.prepare(context)
            return Room.databaseBuilder(context, ContentDatabase::class.java, ContentDatabase.NAME)
                .createFromAsset(ContentDatabase.ASSET_PATH)
                // Allowed on the content database only: it is rebuilt from the asset on any change.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }

        @Provides
        fun provideContentDao(db: ContentDatabase): ContentDao = db.contentDao()
    }
}
