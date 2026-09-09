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
import dev.milan.cryptogram.data.prefs.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { purchases.forEach { handlePurchase(it) } }
        } else if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            scope.launch { setOwned(true) }
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
                .productDetailsList?.firstOrNull() ?: return@launch
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(details)
                            .build(),
                    ),
                )
                .build()
            client.launchBillingFlow(activity, flowParams)
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
    }

    private suspend fun setOwned(owned: Boolean) {
        _removeAdsOwned.value = owned
        settings.setRemoveAdsOwned(owned)
    }
}
