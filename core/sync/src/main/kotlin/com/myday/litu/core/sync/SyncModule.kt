package com.myday.litu.core.sync

import com.myday.litu.core.domain.repository.BackupScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {
    @Binds
    abstract fun bindAccountRepository(impl: FirebaseAccountRepository): AccountRepository

    @Binds
    abstract fun bindBackupScheduler(impl: WorkManagerBackupScheduler): BackupScheduler
}
