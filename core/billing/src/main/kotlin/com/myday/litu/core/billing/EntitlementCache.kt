package com.myday.litu.core.billing

import android.content.Context
import androidx.core.content.edit

internal class EntitlementCache(context: Context) {
    private val prefs = context.getSharedPreferences("entitlement", Context.MODE_PRIVATE)

    var isPro: Boolean
        get() = prefs.getBoolean(KEY_PRO, false)
        set(value) = prefs.edit { putBoolean(KEY_PRO, value) }

    private companion object {
        const val KEY_PRO = "pro"
    }
}
