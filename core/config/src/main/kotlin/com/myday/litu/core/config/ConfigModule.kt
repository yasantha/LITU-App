package com.myday.litu.core.config

import com.myday.litu.core.domain.repository.ConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ConfigModule {
    @Binds
    abstract fun bindConfigRepository(impl: RemoteConfigRepository): ConfigRepository
}
