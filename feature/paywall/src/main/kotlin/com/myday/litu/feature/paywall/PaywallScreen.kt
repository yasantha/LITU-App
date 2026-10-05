package com.myday.litu.feature.paywall

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.myday.litu.core.analytics.AnalyticsEvent
import com.myday.litu.core.analytics.AnalyticsLogger
import com.myday.litu.core.analytics.PaywallSource
import com.myday.litu.core.billing.EntitlementRepository
import com.myday.litu.core.billing.Plan
import com.myday.litu.core.billing.PlanPeriod
import com.myday.litu.core.billing.PlansResult
import com.myday.litu.core.billing.PurchaseOutcome
import com.myday.litu.core.designsystem.component.ButtonVariant
import com.myday.litu.core.designsystem.component.EmptyState
import com.myday.litu.core.designsystem.component.InfoBanner
import com.myday.litu.core.designsystem.component.LegalLinks
import com.myday.litu.core.designsystem.component.LituButton
import com.myday.litu.core.designsystem.component.LituTopBar
import com.myday.litu.core.designsystem.component.PlanCard
import com.myday.litu.core.designsystem.component.ScreenColumn
import com.myday.litu.core.designsystem.component.SkeletonBlock
import com.myday.litu.core.designsystem.component.TopBarNav
import com.myday.litu.core.designsystem.icon.LogoTile
import com.myday.litu.core.designsystem.illustration.Illustration
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable data class PaywallRoute(val source: String = PaywallSource.LOCKED_ITEM.value)

sealed interface PlansUi {
    data object Loading : PlansUi
    data class Ready(val plans: List<Plan>, val selectedId: String) : PlansUi
    data class Unavailable(val message: String) : PlansUi
}

data class PaywallState(val plans: PlansUi = PlansUi.Loading, val purchasing: Boolean = false, val message: String? = null, val done: Boolean = false)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entitlements: EntitlementRepository,
    private val analytics: AnalyticsLogger,
    config: ConfigRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PaywallState())
    val state = mutableState.asStateFlow()

    init {
        val source = PaywallSource.entries.firstOrNull { it.value == savedStateHandle.toRoute<PaywallRoute>().source } ?: PaywallSource.LOCKED_ITEM
        analytics.log(AnalyticsEvent.PaywallView(source, config.config.value.paywallVariant))
        // Already subscribed: go straight on.
        if (entitlements.isPro.value) mutableState.update { it.copy(done = true) } else load()
    }

    fun load() = viewModelScope.launch {
        mutableState.update { it.copy(plans = PlansUi.Loading) }
        mutableState.update {
            it.copy(
                plans = when (val r = entitlements.loadPlans()) {
                    // Annual preselected (highlighted as best value).
                    is PlansResult.Ready -> PlansUi.Ready(r.plans, (r.plans.firstOrNull { p -> p.period == PlanPeriod.ANNUAL } ?: r.plans.first()).id)
                    is PlansResult.Unavailable -> PlansUi.Unavailable(r.reason)
                },
            )
        }
    }

    fun select(id: String) = mutableState.update { s -> (s.plans as? PlansUi.Ready)?.let { s.copy(plans = it.copy(selectedId = id)) } ?: s }

    fun purchase(activity: Activity?) {
        val ready = state.value.plans as? PlansUi.Ready ?: return
        val plan = ready.plans.first { it.id == ready.selectedId }
        activity ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(purchasing = true, message = null) }
            when (val r = entitlements.purchase(activity, plan)) {
                is PurchaseOutcome.Success -> {
                    analytics.log(if (r.startedTrial) AnalyticsEvent.TrialStart(r.productId) else AnalyticsEvent.Purchase(r.productId))
                    mutableState.update { it.copy(purchasing = false, done = true) }
                }
                PurchaseOutcome.Cancelled -> mutableState.update { it.copy(purchasing = false) }
                is PurchaseOutcome.Failed -> mutableState.update { it.copy(purchasing = false, message = r.message) }
            }
        }
    }

    fun restore() = viewModelScope.launch {
        mutableState.update { it.copy(purchasing = true, message = null) }
        when (val r = entitlements.restore()) {
            is PurchaseOutcome.Success -> mutableState.update { it.copy(purchasing = false, done = true) }
            is PurchaseOutcome.Failed -> mutableState.update { it.copy(purchasing = false, message = r.message) }
            PurchaseOutcome.Cancelled -> mutableState.update { it.copy(purchasing = false) }
        }
    }
}

fun NavGraphBuilder.paywallScreen(onClose: () -> Unit, onSubscribed: () -> Unit) {
    composable<PaywallRoute> { PaywallScreen(onClose, onSubscribed) }
}

/** S06: annual preselected, real prices from Play, trial, renewal and cancellation shown. */
@Composable
internal fun PaywallScreen(onClose: () -> Unit, onSubscribed: () -> Unit, viewModel: PaywallViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.done) { if (s.done) onSubscribed() }
    val c = LituTheme.colors
    val activity = LocalActivity.current
    val uri = LocalUriHandler.current
    val ready = s.plans as? PlansUi.Ready
    val selected = ready?.plans?.firstOrNull { it.id == ready.selectedId }
    ScreenColumn(
        topBar = { LituTopBar("", nav = TopBarNav.CLOSE, onNav = onClose) },
        bottomBar = {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (ready != null && selected != null) {
                    LituButton(
                        if (selected.trialDays != null) "Start ${selected.trialDays}-day free trial" else "Subscribe",
                        { viewModel.purchase(activity) },
                        loading = s.purchasing,
                    )
                    Text(smallPrint(selected), style = LituTheme.type.caption, color = c.textSecondary, textAlign = TextAlign.Center)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LituButton("Restore purchases", viewModel::restore, variant = ButtonVariant.TEXT, fillWidth = false)
                    LituButton("Terms", { uri.openUri(LegalLinks.TERMS) }, variant = ButtonVariant.TEXT, fillWidth = false)
                    LituButton("Privacy", { uri.openUri(LegalLinks.PRIVACY) }, variant = ButtonVariant.TEXT, fillWidth = false)
                }
                Text(LegalLinks.DISCLAIMER, style = LituTheme.type.caption, color = c.textSecondary, textAlign = TextAlign.Center)
            }
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LogoTile(56.dp)
            Text("Pass your Life in the UK test first time", style = LituTheme.type.headline, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        }
        Benefit(Icons.AutoMirrored.Rounded.MenuBook, "Full question bank by chapter", c.primaryContainer, c.primary)
        Benefit(Icons.Rounded.LibraryBooks, "Unlimited mock tests", c.coralContainer, c.onCoralContainer)
        Benefit(Icons.Rounded.Replay, "Review every mistake", c.amberContainer, c.warning)
        Benefit(Icons.AutoMirrored.Rounded.VolumeUp, "Audio for every question", c.plumContainer, c.onPlumContainer)
        Benefit(Icons.Rounded.Block, "No ads", c.blueContainer, c.secondary)
        s.message?.let { InfoBanner(it, icon = Icons.Rounded.Info) }
        when (val p = s.plans) {
            PlansUi.Loading -> repeat(2) { SkeletonBlock(Modifier.fillMaxWidth().height(72.dp)) }
            is PlansUi.Unavailable -> EmptyState(Illustration.BUS_STOP, "Plans are not available", p.message, actionLabel = "Retry", onAction = viewModel::load)
            is PlansUi.Ready -> {
                if (p.plans.any { it.isTestStore }) InfoBanner("Test store: no real payment is taken in this debug build.", icon = Icons.Rounded.Info)
                p.plans.sortedBy { it.period != PlanPeriod.ANNUAL }.forEach { plan ->
                    PlanCard(
                        title = when (plan.period) { PlanPeriod.ANNUAL -> "Annual"; PlanPeriod.MONTHLY -> "Monthly"; else -> "Plan" },
                        priceLine = when (plan.period) {
                            PlanPeriod.ANNUAL -> "${plan.price} / year" + (plan.pricePerMonth?.let { " · $it / month" } ?: "")
                            PlanPeriod.MONTHLY -> "${plan.price} / month"
                            else -> plan.price
                        },
                        selected = plan.id == p.selectedId,
                        onClick = { viewModel.select(plan.id) },
                        badge = if (plan.period == PlanPeriod.ANNUAL) "Best value" else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, text: String, container: Color, tint: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = CircleShape, color = container, modifier = Modifier.size(40.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.padding(9.dp))
        }
        Text(text, style = LituTheme.type.body, color = LituTheme.colors.textPrimary)
    }
}

private fun smallPrint(plan: Plan): String {
    val period = when (plan.period) { PlanPeriod.ANNUAL -> "per year"; PlanPeriod.MONTHLY -> "per month"; else -> "" }
    val trial = plan.trialDays?.let { "Free for $it days, then " } ?: ""
    return "$trial${plan.price} $period. Renews automatically. Cancel anytime in Google Play.".replace("  ", " ")
}
