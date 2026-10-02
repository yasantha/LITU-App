package com.myday.litu.core.billing

import android.content.Context
import androidx.core.content.edit

internal class EntitlementCache(context: Context) {
    private val prefs = context.getSharedPreferences("entitlement", Context.MODE_PRIVATE)

    var isPro: Boolean
        get() = prefs.getBoolean(KEY_PRO, false)
        set(value) = prefs.edit {
            putBoolean(KEY_PRO, value)
            if (value) putBoolean(KEY_EVER_PRO, true)
        }

    val everPro: Boolean get() = prefs.getBoolean(KEY_EVER_PRO, false)

    private companion object {
        const val KEY_PRO = "pro"
        const val KEY_EVER_PRO = "ever_pro"
    }
}
