package dev.milan.cryptogram.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dev.milan.cryptogram.BuildConfig
import dev.milan.cryptogram.data.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val PRODUCT_REMOVE_ADS = "remove_ads"

/**
 * Play Billing for the single non-consumable `remove_ads` (design doc section 7).
 * The entitlement is mirrored into DataStore so the rest of the app reads it offline.
 */
class BillingManager(
    context: Context,
    private val settings: SettingsStore,
    private val scope: CoroutineScope,
) {
    private val appContext = context.applicationContext

    private val _removeAdsOwned = MutableStateFlow(false)
    val removeAdsOwned: StateFlow<Boolean> = _removeAdsOwned.asStateFlow()

    /** Short user-facing outcome messages for the purchase button. */
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK ->
                if (purchases != null) scope.launch { purchases.forEach { handlePurchase(it) } }

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                scope.launch { setOwned(true); _messages.emit("Ads already removed.") }

            BillingClient.BillingResponseCode.USER_CANCELED -> Unit

            else -> _messages.tryEmit("Purchase failed (${result.responseCode}).")
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(purchasesListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        .build()

    fun connect() {
        if (client.isReady) {
            scope.launch { refreshOwned() }
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { refreshOwned() }
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }

    suspend fun refreshOwned() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryPurchasesAsync(params)
        val owned = result.purchasesList.any {
            PRODUCT_REMOVE_ADS in it.products &&
                it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        result.purchasesList.forEach { handlePurchase(it) }
        setOwned(owned)
    }

    fun launchPurchase(activity: Activity) {
        scope.launch {
            if (!client.isReady) {
                _messages.emit("Store isn't connected yet — try again in a moment.")
                connect()
                return@launch
            }
            val productParams = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_REMOVE_ADS)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    ),
                )
                .build()
            val details = client.queryProductDetails(productParams)
                .productDetailsList?.firstOrNull()

            if (details == null) {
                if (BuildConfig.DEBUG) {
                    setOwned(true)
                    _messages.emit("Ads removed (debug — no Play product configured).")
                } else {
                    _messages.emit("This purchase isn't available right now.")
                }
                return@launch
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build(),
                    ),
                )
                .build()
            val result = client.launchBillingFlow(activity, flowParams)
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _messages.emit("Couldn't open the purchase (${result.responseCode}).")
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (PRODUCT_REMOVE_ADS !in purchase.products) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build(),
            )
        }
        setOwned(true)
        _messages.emit("Ads removed. Thank you.")
    }

    private suspend fun setOwned(owned: Boolean) {
        _removeAdsOwned.value = owned
        settings.setRemoveAdsOwned(owned)
    }
}
