package com.zchat.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.zchat.app.data.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Billing Manager for handling in-app purchases
 *
 * Supported payment methods:
 * - Google Play Billing (Credit/Debit cards, PayPal, etc.)
 *
 * For bank cards support:
 * - Belarus: PriorBank, Belarusbank, Alfa-Bank (via Google Pay)
 * - Russia: Sberbank, Tinkoff, Alfa-Bank (via Google Pay)
 * - USA/Europe: Visa, Mastercard, American Express
 * - International: PayPal, Google Pay
 *
 * Funds flow:
 * 1. User purchases subscription -> Google Play processes payment
 * 2. Google takes ~15-30% fee
 * 3. Remaining funds go to your Google Play Merchant Account
 * 4. You can withdraw to your bank account monthly
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val prefsManager = PreferencesManager(context)
    private var billingClient: BillingClient? = null

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState

    // Product IDs for subscriptions
    companion object {
        // BASIC Plan
        const val SKU_BASIC_MONTHLY = "goodok_basic_monthly"      // $2/month
        const val SKU_BASIC_FOREVER = "goodok_basic_forever"      // $6 one-time

        // GOODPLAN
        const val SKU_GOODPLAN_MONTHLY = "goodok_goodplan_monthly" // $5/month
        const val SKU_GOODPLAN_FOREVER = "goodok_goodplan_forever" // $15 one-time
    }

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        data class Success(val sku: String, val productId: String = sku) : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    // Cached product details for quick access
    private val cachedProducts = mutableMapOf<String, ProductDetails>()

    init {
        initBillingClient()
    }

    private fun initBillingClient() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
    }

    fun startConnection(onConnected: () -> Unit) {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection
                startConnection(onConnected)
            }
        })
    }

    fun getProducts(): List<ProductDetails> {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_BASIC_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_BASIC_FOREVER)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_GOODPLAN_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_GOODPLAN_FOREVER)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        var products = listOf<ProductDetails>()
        billingClient?.queryProductDetailsAsync(params) { _, productDetailsList ->
            products = productDetailsList
        }
        return products
    }

    fun purchase(activity: Activity, productDetails: ProductDetails, offerToken: String? = null) {
        val productDetailsParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
        
        // Add offer token for subscriptions
        if (offerToken != null) {
            productDetailsParamsBuilder.setOfferToken(offerToken)
        }
        
        val productDetailsParams = productDetailsParamsBuilder.build()

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()

        billingClient?.launchBillingFlow(activity, billingFlowParams)
    }

    /**
     * Query all products and cache them
     */
    fun queryAllProducts(onResult: ((List<ProductDetails>) -> Unit)? = null) {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_BASIC_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_BASIC_FOREVER)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_GOODPLAN_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SKU_GOODPLAN_FOREVER)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { _, productDetailsList ->
            // Cache products
            productDetailsList.forEach { details ->
                cachedProducts[details.productId] = details
            }
            onResult?.invoke(productDetailsList)
        }
    }

    /**
     * Get cached product details by product ID
     */
    fun getProductDetails(productId: String): ProductDetails? {
        return cachedProducts[productId]
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _purchaseState.value = PurchaseState.Error("Purchase cancelled")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Already owned, acknowledge and grant
                _purchaseState.value = PurchaseState.Success("already_owned")
                grantPremium("BASIC") // Check actual SKU
            }
            else -> {
                _purchaseState.value = PurchaseState.Error(billingResult.debugMessage)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Grant premium based on SKU
            val sku = purchase.products.firstOrNull() ?: ""
            when (sku) {
                SKU_BASIC_MONTHLY, SKU_BASIC_FOREVER -> grantPremium("BASIC")
                SKU_GOODPLAN_MONTHLY, SKU_GOODPLAN_FOREVER -> grantPremium("GOODPLAN")
            }

            // Acknowledge purchase
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient?.acknowledgePurchase(params) { _purchaseState.value = PurchaseState.Success(sku) }
            } else {
                _purchaseState.value = PurchaseState.Success(sku)
            }
        }
    }

    private fun grantPremium(type: String) {
        prefsManager.isPremium = true
        prefsManager.premiumType = type
        prefsManager.premiumExpiry = if (type == "BASIC_FOREVER" || type == "GOODPLAN_FOREVER") {
            Long.MAX_VALUE // Forever
        } else {
            System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
        }
    }

    fun restorePurchases(onRestored: (List<Purchase>) -> Unit) {
        billingClient?.queryPurchasesAsync(QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()) { _, purchasesList ->
            purchasesList.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    handlePurchase(purchase)
                }
            }
            onRestored(purchasesList)
        }
    }

    fun endConnection() {
        billingClient?.endConnection()
    }
}

/**
 * Payment information for different regions:
 *
 * BELARUS:
 * - Cards: Visa, Mastercard, Belkart (via Google Pay)
 * - Banks: PriorBank, Belarusbank, Alfa-Bank, BPS-Sberbank
 * - Note: Some sanctions may apply, Google Play works but some features limited
 *
 * RUSSIA:
 * - Cards: Mir, UnionPay (Visa/Mastercard suspended due to sanctions)
 * - Google Play may have restrictions
 * - Alternative: Consider RuStore or local payment processors
 *
 * USA/EUROPE:
 * - Cards: Visa, Mastercard, American Express, Discover
 * - Digital: Google Pay, PayPal
 * - All features fully supported
 *
 * FUNDS FLOW:
 * 1. User pays via Google Play
 * 2. Google processes payment (takes 15-30% fee)
 * 3. Funds go to Google Play Console merchant account
 * 4. Payout to your bank account: Monthly (if balance > $25)
 * 5. Payout methods: Bank transfer, Wire transfer
 *
 * ALTERNATIVES for Belarus/Russia:
 * - Stripe (may not work in Belarus/Russia)
 * - YooKassa (Russia)
 * - BePaid (Belarus)
 * - WebMoney, QIWI, Yandex.Money
 */
