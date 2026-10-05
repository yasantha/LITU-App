package com.myday.litu.core.ads

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.domain.repository.ConfigRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdMob for the free version. Ads show only to free users, only after the Google consent flow
 * (required for UK and EEA users), and only when Remote Config `ads_enabled` is on. Pro removes ads.
 */
@Singleton
class AdController @Inject constructor(
    @ApplicationContext private val context: Context,
    entitlements: EntitlementRepository,
    config: ConfigRepository,
) {
    private val debug = context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    private val consented = MutableStateFlow(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    internal val bannerUnitId: String? = AdIds.banner(debug)
    private val interstitialUnitId: String? = AdIds.interstitial(debug)

    /** True when this user should see ads now. */
    val showAds: StateFlow<Boolean> = combine(entitlements.isPro, config.config, consented) { pro, cfg, ok ->
        !pro && cfg.adsEnabled && ok && bannerUnitId != null
    }.stateIn(scope, SharingStarted.Eagerly, false)

    private var initialised = false
    private var interstitial: InterstitialAd? = null
    private var lastInterstitialAt = 0L

    /** Runs the consent flow once per launch, then starts the SDK. Call from Home, after onboarding. */
    fun requestConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                    error?.let { Log.w(TAG, "Consent form: ${it.message}") }
                    if (info.canRequestAds()) start()
                }
            },
            { error -> Log.w(TAG, "Consent update failed: ${error.message}") },
        )
        // Consent is often already settled from an earlier launch.
        if (info.canRequestAds()) start()
    }

    private fun start() {
        // No ad units configured (a release build without IDs): stay off entirely.
        if (initialised || bannerUnitId == null) return
        initialised = true
        MobileAds.initialize(context)
        consented.value = true
        loadInterstitial()
    }

    private fun loadInterstitial() {
        val unit = interstitialUnitId ?: return
        if (!showAds.value && !consented.value) return
        InterstitialAd.load(context, unit, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(ad: InterstitialAd) {
                interstitial = ad
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                interstitial = null
            }
        })
    }

    /**
     * Shows an interstitial at a natural break (leaving a finished session or mock results), at most
     * once every [MIN_INTERVAL_MS]. Always calls [onDone], whether or not an ad was shown.
     */
    fun showInterstitialAtBreak(activity: Activity?, onDone: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val due = lastInterstitialAt == 0L || now - lastInterstitialAt >= MIN_INTERVAL_MS
        val canShow = showAds.value && due
        if (activity == null || ad == null || !canShow) {
            if (ad == null) loadInterstitial()
            return onDone()
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = finish()
            override fun onAdFailedToShowFullScreenContent(error: AdError) = finish()
            private fun finish() {
                interstitial = null
                loadInterstitial()
                onDone()
            }
        }
        lastInterstitialAt = now
        ad.show(activity)
    }

    /** UK and EEA users must be able to change their ad consent from inside the app. */
    fun privacyOptionsRequired(): Boolean =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) = UserMessagingPlatform.showPrivacyOptionsForm(activity) {}

    private companion object {
        const val TAG = "Ads"
        const val MIN_INTERVAL_MS = 5 * 60 * 1000L
    }
}
