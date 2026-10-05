package com.myday.litu.core.ads

/**
 * Google's public sample units always serve test creatives, so debug builds never hit real units
 * (which risks invalid-traffic flags on the AdMob account). Release builds use this app's own
 * units; if none are configured, ads stay off rather than serving test ads to real users.
 */
internal object AdIds {
    private const val BANNER_TEST = "ca-app-pub-3940256099942544/6300978111"
    private const val INTERSTITIAL_TEST = "ca-app-pub-3940256099942544/1033173712"

    fun banner(debug: Boolean): String? = if (debug) BANNER_TEST else BuildConfig.ADMOB_BANNER_ID.ifBlank { null }

    fun interstitial(debug: Boolean): String? = if (debug) INTERSTITIAL_TEST else BuildConfig.ADMOB_INTERSTITIAL_ID.ifBlank { null }
}
