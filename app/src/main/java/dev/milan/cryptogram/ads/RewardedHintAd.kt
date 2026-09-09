package dev.milan.cryptogram.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Keeps a single rewarded ad preloaded for the "watch to get a hint" button
 * (design doc section 7). Available regardless of the remove-ads entitlement.
 */
class RewardedHintAd(
    private val appContext: Context,
    private val unitId: String,
) {
    private var ad: RewardedAd? = null
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    fun load() {
        if (ad != null) return
        RewardedAd.load(
            appContext,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loadedAd: RewardedAd) {
                    ad = loadedAd
                    _loaded.value = true
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    _loaded.value = false
                }
            },
        )
    }

    /** Shows the ad; [onReward] runs on onUserEarnedReward. Reloads afterwards. */
    fun show(activity: Activity, onReward: () -> Unit) {
        val current = ad ?: return
        ad = null
        _loaded.value = false
        current.show(activity) { onReward() }
        load()
    }
}
