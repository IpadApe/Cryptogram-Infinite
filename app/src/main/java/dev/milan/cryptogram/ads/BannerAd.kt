package dev.milan.cryptogram.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import dev.milan.cryptogram.BuildConfig
import dev.milan.cryptogram.ui.rememberAppContainer

/**
 * Anchored bottom banner. Renders nothing (zero height) when ads are removed or the
 * SDK is not yet initialised, so callers never reserve space (design doc section 7).
 * Must never be used on the Play screen.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val container = rememberAppContainer()
    val adsInitialized by container.adManager.initialized.collectAsStateWithLifecycle()
    val removeAdsOwned by container.settingsStore.removeAdsOwned
        .collectAsStateWithLifecycle(initialValue = false)

    if (removeAdsOwned || !adsInitialized) return

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BuildConfig.BANNER_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        },
    )
}
