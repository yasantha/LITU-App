package com.myday.litu.core.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.myday.litu.core.config.FirebaseAvailability
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAnalyticsLogger @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnalyticsLogger {
    private val analytics: FirebaseAnalytics? by lazy {
        if (FirebaseAvailability.isConfigured(context)) FirebaseAnalytics.getInstance(context) else null
    }

    override fun log(event: AnalyticsEvent) {
        val fa = analytics
        if (fa == null) {
            Log.d(TAG, "${event.name} ${event.params}")
            return
        }
        val bundle = Bundle()
        event.params.forEach { (k, v) ->
            when (v) {
                is Boolean -> bundle.putLong(k, if (v) 1 else 0)
                is Int -> bundle.putLong(k, v.toLong())
                is Long -> bundle.putLong(k, v)
                else -> bundle.putString(k, v.toString())
            }
        }
        fa.logEvent(event.name, bundle)
    }

    private companion object {
        const val TAG = "Analytics"
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    @Binds
    abstract fun bindAnalyticsLogger(impl: FirebaseAnalyticsLogger): AnalyticsLogger
}
