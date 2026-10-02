package com.myday.litu.di

import com.myday.litu.BuildConfig
import com.myday.litu.core.domain.repository.AppInfo
import com.myday.litu.core.domain.repository.ReminderScheduler
import com.myday.litu.reminder.WorkManagerReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    @Binds
    abstract fun bindReminderScheduler(impl: WorkManagerReminderScheduler): ReminderScheduler

    companion object {
        /** Days use the phone's local date (spec 9.4). */
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Singleton
        fun provideAppInfo(): AppInfo = object : AppInfo {
            override val versionName = BuildConfig.VERSION_NAME
            override val versionCode = BuildConfig.VERSION_CODE.toLong()
        }
    }
}
