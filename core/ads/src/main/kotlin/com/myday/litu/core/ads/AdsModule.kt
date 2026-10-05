package com.myday.litu.core.ads

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets the app shell reach the singleton without a ViewModel. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AdsEntryPoint {
    fun adController(): AdController
}
