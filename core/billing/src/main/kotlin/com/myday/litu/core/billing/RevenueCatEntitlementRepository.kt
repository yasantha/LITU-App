package com.myday.litu.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitCustomerInfo
import com.revenuecat.purchases.awaitLogIn
import com.revenuecat.purchases.awaitOfferings
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.awaitRestore
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

internal class RevenueCatEntitlementRepository(
    context: Context,
    apiKey: String,
) : EntitlementRepository {
    private val cache = EntitlementCache(context)
    private val pro = MutableStateFlow(cache.isPro)
    override val isPro: StateFlow<Boolean> = pro.asStateFlow()
    override val lostPro: Boolean get() = cache.everPro && !pro.value

    init {
        Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).build())
        Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { update(it) }
    }

    private fun update(info: CustomerInfo) {
        val active = info.entitlements[ENTITLEMENT_PRO]?.isActive == true
        cache.isPro = active
        pro.value = active
    }

    override suspend fun refresh() {
        runCatching { update(Purchases.sharedInstance.awaitCustomerInfo()) }
            .onFailure { Log.w(TAG, "Could not refresh customer info; keeping cached entitlement", it) }
    }

    override suspend fun identify(uid: String) {
        runCatching { update(Purchases.sharedInstance.awaitLogIn(uid).customerInfo) }
            .onFailure { Log.w(TAG, "RevenueCat logIn failed", it) }
    }

    override suspend fun loadPlans(): PlansResult = try {
        val offering = Purchases.sharedInstance.awaitOfferings().current
            ?: return PlansResult.Unavailable("No plans are available right now.")
        val plans = offering.availablePackages.map { pkg ->
            val product = pkg.product
            Plan(
                id = pkg.identifier,
                productId = product.id,
                period = when (pkg.packageType) {
                    PackageType.MONTHLY -> PlanPeriod.MONTHLY
                    PackageType.ANNUAL -> PlanPeriod.ANNUAL
                    else -> PlanPeriod.OTHER
                },
                price = product.price.formatted,
                pricePerMonth = if (pkg.packageType == PackageType.ANNUAL) product.pricePerMonth(Locale.getDefault())?.formatted else null,
                trialDays = product.trialDays(),
            )
        }
        if (plans.isEmpty()) PlansResult.Unavailable("No plans are available right now.") else PlansResult.Ready(plans)
    } catch (e: PurchasesException) {
        PlansResult.Unavailable(e.message ?: "Google Play billing is not available.")
    }

    override suspend fun purchase(activity: Activity, plan: Plan): PurchaseOutcome = try {
        val offering = Purchases.sharedInstance.awaitOfferings().current
        val pkg = offering?.availablePackages?.firstOrNull { it.identifier == plan.id }
            ?: return PurchaseOutcome.Failed("This plan is no longer available.")
        val result = Purchases.sharedInstance.awaitPurchase(PurchaseParams.Builder(activity, pkg).build())
        update(result.customerInfo)
        PurchaseOutcome.Success(plan.productId, startedTrial = plan.trialDays != null)
    } catch (e: PurchasesTransactionException) {
        if (e.userCancelled) PurchaseOutcome.Cancelled else PurchaseOutcome.Failed(e.message ?: "Purchase failed.")
    } catch (e: PurchasesException) {
        PurchaseOutcome.Failed(e.message ?: "Purchase failed.")
    }

    override suspend fun restore(): PurchaseOutcome = try {
        val info = Purchases.sharedInstance.awaitRestore()
        update(info)
        if (pro.value) PurchaseOutcome.Success("restore", startedTrial = false)
        else PurchaseOutcome.Failed("No active subscription was found for this Google account.")
    } catch (e: PurchasesException) {
        PurchaseOutcome.Failed(e.message ?: "Restore failed.")
    }

    private fun StoreProduct.trialDays(): Int? = defaultOption?.freePhase?.billingPeriod?.let(::days)

    private fun days(p: Period): Int = when (p.unit) {
        Period.Unit.DAY -> p.value
        Period.Unit.WEEK -> p.value * 7
        Period.Unit.MONTH -> p.value * 30
        Period.Unit.YEAR -> p.value * 365
        else -> p.value
    }

    private companion object {
        const val TAG = "Billing"
    }
}
