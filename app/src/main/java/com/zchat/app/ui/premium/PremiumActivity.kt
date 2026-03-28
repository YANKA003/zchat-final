package com.zchat.app.ui.premium

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import com.zchat.app.R
import com.zchat.app.billing.BillingManager
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityPremiumBinding
import kotlinx.coroutines.launch

class PremiumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPremiumBinding
    private lateinit var repository: Repository
    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)
        billingManager = BillingManager(this)

        setSupportActionBar(binding.toolbarPremium)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.premium)

        setupBilling()
        setupPlans()
        updatePremiumStatus()
    }

    private fun setupBilling() {
        billingManager.startConnection {
            // Query products after connection
            billingManager.queryAllProducts()

            lifecycleScope.launch {
                billingManager.purchaseState.collect { state ->
                    when (state) {
                        is BillingManager.PurchaseState.Success -> {
                            Toast.makeText(
                                this@PremiumActivity,
                                "${getString(R.string.success)}! ${state.productId}",
                                Toast.LENGTH_LONG
                            ).show()
                            updatePremiumStatus()
                        }
                        is BillingManager.PurchaseState.Error -> {
                            Toast.makeText(
                                this@PremiumActivity,
                                "${getString(R.string.error)}: ${state.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupPlans() {
        // BASIC plan
        binding.cardBasic.apply {
            binding.tvBasicTitle.text = getString(R.string.basic_plan)
            binding.tvBasicMonthly.text = "$2${getString(R.string.per_month)}"
            binding.tvBasicForever.text = "$6 ${getString(R.string.forever)}"
            binding.tvBasicFeatures.text = getString(R.string.basic_features)
        }

        // GOODPLAN
        binding.cardGoodplan.apply {
            binding.tvGoodplanTitle.text = getString(R.string.goodplan)
            binding.tvGoodplanMonthly.text = "$5${getString(R.string.per_month)}"
            binding.tvGoodplanForever.text = "$15 ${getString(R.string.forever)}"
            binding.tvGoodplanFeatures.text = getString(R.string.goodplan_features)
        }

        // BASIC monthly button
        binding.btnBasicMonthly.setOnClickListener {
            purchasePlan(BillingManager.SKU_BASIC_MONTHLY)
        }

        // BASIC forever button
        binding.btnBasicForever.setOnClickListener {
            purchasePlan(BillingManager.SKU_BASIC_FOREVER)
        }

        // GOODPLAN monthly button
        binding.btnGoodplanMonthly.setOnClickListener {
            purchasePlan(BillingManager.SKU_GOODPLAN_MONTHLY)
        }

        // GOODPLAN forever button
        binding.btnGoodplanForever.setOnClickListener {
            purchasePlan(BillingManager.SKU_GOODPLAN_FOREVER)
        }

        // Restore purchase
        binding.btnRestore.setOnClickListener {
            billingManager.restorePurchases { purchases ->
                if (purchases.isEmpty()) {
                    Toast.makeText(this, "No purchases found", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "${purchases.size} purchases restored", Toast.LENGTH_SHORT).show()
                    updatePremiumStatus()
                }
            }
        }
    }

    private fun purchasePlan(productId: String) {
        val productDetails = billingManager.getProductDetails(productId)

        if (productDetails != null) {
            // Get offer token for subscriptions
            val offerToken = if (productDetails.productType == BillingClient.ProductType.SUBS) {
                productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
            } else {
                null
            }
            billingManager.purchase(this, productDetails, offerToken)
        } else {
            // Product not found in Play Console - show demo purchase
            Toast.makeText(this, "Demo mode: Product not configured in Play Console", Toast.LENGTH_LONG).show()
            // For testing: grant premium manually
            val type = if (productId.contains("goodplan", ignoreCase = true)) "GOODPLAN" else "BASIC"
            repository.isPremium = true
            repository.premiumType = type
            updatePremiumStatus()
        }
    }

    private fun updatePremiumStatus() {
        if (repository.isPremium) {
            binding.tvCurrentPlan.text = "${getString(R.string.premium)}: ${repository.premiumType}"
            binding.tvCurrentPlan.visibility = android.view.View.VISIBLE
        } else {
            binding.tvCurrentPlan.visibility = android.view.View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.endConnection()
    }
}
