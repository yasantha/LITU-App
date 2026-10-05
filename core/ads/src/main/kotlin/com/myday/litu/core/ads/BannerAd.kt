package com.myday.litu.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** A standard 320×50 banner; renders nothing for Pro users or before consent. */
@Composable
fun BannerAd(controller: AdController, background: Color, modifier: Modifier = Modifier) {
    val show by controller.showAds.collectAsStateWithLifecycle()
    val unit = controller.bannerUnitId
    if (!show || unit == null) return
    val context = LocalContext.current
    val view = remember(unit) {
        AdView(context).apply {
            setAdSize(AdSize.BANNER)
            adUnitId = unit
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(view) { onDispose { view.destroy() } }
    Box(modifier.fillMaxWidth().height(50.dp).background(background), contentAlignment = Alignment.Center) {
        AndroidView(factory = { view })
    }
}
