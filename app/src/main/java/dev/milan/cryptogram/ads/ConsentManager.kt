package dev.milan.cryptogram.ads

import android.app.Activity
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dev.milan.cryptogram.data.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Google User Messaging Platform consent flow (design doc section 7).
 * Ads are only initialised once consent has been gathered (or is not required).
 */
class ConsentManager(
    private val settings: SettingsStore,
    private val adManager: AdManager,
) {
    fun gatherConsent(activity: Activity, scope: CoroutineScope) {
        val info: ConsentInformation = UserMessagingPlatform.getConsentInformation(activity)
        val params = ConsentRequestParameters.Builder().build()

        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    onConsentResolved(activity, info, scope)
                }
            },
            {
                // Update failed: proceed with whatever consent state we already have.
                onConsentResolved(activity, info, scope)
            },
        )
    }

    private fun onConsentResolved(
        activity: Activity,
        info: ConsentInformation,
        scope: CoroutineScope,
    ) {
        if (info.canRequestAds()) {
            adManager.initializeOnce(activity.applicationContext)
        }
        scope.launch { settings.setConsentObtained(true) }
    }
}
