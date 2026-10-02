package com.myday.litu.core.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Used when no RevenueCat key is configured. Debug builds get a local test store (clearly marked
 * on the paywall) so the purchase flow can be exercised; release builds report billing unavailable.
 */
internal class LocalEntitlementRepository(
    context: Context,
    private val testStore: Boolean,
) : EntitlementRepository {
    private val cache = EntitlementCache(context)
    private val pro = MutableStateFlow(testStore && cache.isPro)
    override val isPro: StateFlow<Boolean> = pro.asStateFlow()
    override val lostPro: Boolean get() = cache.everPro && !pro.value

    override suspend fun loadPlans(): PlansResult {
        if (!testStore) return PlansResult.Unavailable("Google Play billing is not available.")
        delay(400) // Shows the skeleton state like a real store call.
        return PlansResult.Ready(
            listOf(
                Plan("annual", "litu_pro_annual", PlanPeriod.ANNUAL, "£34.99", "£2.92", 7, isTestStore = true),
                Plan("monthly", "litu_pro_monthly", PlanPeriod.MONTHLY, "£7.99", null, 7, isTestStore = true),
            ),
        )
    }

    override suspend fun purchase(activity: Activity, plan: Plan): PurchaseOutcome {
        if (!testStore) return PurchaseOutcome.Failed("Google Play billing is not available.")
        cache.isPro = true
        pro.value = true
        return PurchaseOutcome.Success(plan.productId, startedTrial = true)
    }

    override suspend fun restore(): PurchaseOutcome =
        if (pro.value) PurchaseOutcome.Success("restore", false) else PurchaseOutcome.Failed("No active subscription was found.")

    override suspend fun identify(uid: String) = Unit
    override suspend fun refresh() = Unit

    /** Test-store only: lets developers return to the free tier from Settings. */
    fun reset() {
        cache.isPro = false
        pro.value = false
    }
}
