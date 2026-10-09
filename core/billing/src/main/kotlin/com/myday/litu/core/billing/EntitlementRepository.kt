package com.myday.litu.core.billing

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/** Entitlement name in RevenueCat; granted by any product in spec section 4. */
const val ENTITLEMENT_PRO = "pro"

enum class PlanPeriod(val months: Int) { MONTHLY(1), THREE_MONTHS(3), ANNUAL(12), OTHER(0) }

/** A plan as shown on the paywall. Prices always come from Google Play, never hard-coded. */
data class Plan(
    val id: String,
    val productId: String,
    val period: PlanPeriod,
    val price: String,
    val pricePerMonth: String?,
    val trialDays: Int?,
    val isTestStore: Boolean = false,
)

sealed interface PlansResult {
    data class Ready(val plans: List<Plan>) : PlansResult
    data class Unavailable(val reason: String) : PlansResult
}

sealed interface PurchaseOutcome {
    data class Success(val productId: String, val startedTrial: Boolean) : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data class Failed(val message: String) : PurchaseOutcome
}

interface EntitlementRepository {
    /** Cached so a user with no signal keeps access; RevenueCat handles grace periods. */
    val isPro: StateFlow<Boolean>

    /** True when this user had Pro before but no longer does, e.g. the trial ended (S01 routes to S06). */
    val lostPro: Boolean

    suspend fun loadPlans(): PlansResult
    suspend fun purchase(activity: Activity, plan: Plan): PurchaseOutcome
    suspend fun restore(): PurchaseOutcome

    /** Uses the Firebase uid as the RevenueCat customer, so entitlements follow a linked account. */
    suspend fun identify(uid: String)
    suspend fun refresh()

    companion object {
        const val MANAGE_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"
    }
}
