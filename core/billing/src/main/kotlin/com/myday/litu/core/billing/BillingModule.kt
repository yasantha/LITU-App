package com.myday.litu.core.billing

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun provideEntitlementRepository(@ApplicationContext context: Context): EntitlementRepository =
        if (BuildConfig.REVENUECAT_API_KEY.isNotBlank()) {
            RevenueCatEntitlementRepository(context, BuildConfig.REVENUECAT_API_KEY)
        } else {
            LocalEntitlementRepository(context, testStore = isDebuggable(context))
        }

    private fun isDebuggable(context: Context) =
        context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0
}
