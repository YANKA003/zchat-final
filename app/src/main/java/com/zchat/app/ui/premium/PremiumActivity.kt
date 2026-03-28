package com.zchat.app.ui.premium

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.zchat.app.R
import com.zchat.app.billing.BillingManager
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityPremiumBinding
import com.zchat.app.util.LanguageHelper
import kotlinx.coroutines.launch

class PremiumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPremiumBinding
    private lateinit var repository: Repository
    private var billingManager: BillingManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLanguage()
        applyTheme()
        super.onCreate(savedInstanceState)

        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setupToolbar()
        setupBilling()
        setupPlans()
        updatePremiumStatus()
    }

    private fun applyLanguage() {
        try {
            val repo = Repository(applicationContext)
            LanguageHelper.setLanguage(this, repo.language)
        } catch (e: Exception) {
            Log.e("PremiumActivity", "Error applying language", e)
        }
    }

    private fun applyTheme() {
        try {
            val repo = Repository(applicationContext)
            when (repo.theme) {
                0 -> setTheme(R.style.Theme_GOODOK_Classic)
                1 -> setTheme(R.style.Theme_GOODOK_Modern)
                2 -> setTheme(R.style.Theme_GOODOK_Neon)
                3 -> setTheme(R.style.Theme_GOODOK_Childish)
            }
        } catch (e: Exception) {
            setTheme(R.style.Theme_GOODOK_Classic)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarPremium)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.premium)
    }

    private fun setupBilling() {
        try {
            billingManager = BillingManager(this)
            billingManager?.startConnection {
                // Billing client connected
                billingManager?.queryAllProducts()

                lifecycleScope.launch {
                    billingManager?.purchaseState?.collect { state ->
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
        } catch (e: Exception) {
            Log.e("PremiumActivity", "Error setting up billing", e)
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
            billingManager?.restorePurchases { purchases ->
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
        try {
            val productDetails = billingManager?.getProductDetails(productId)

            if (productDetails != null) {
                val offerToken = if (productDetails.productType == com.android.billingclient.api.BillingClient.ProductType.SUBS) {
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                } else {
                    null
                }
                billingManager?.purchase(this, productDetails, offerToken)
            } else {
                // Product not found in Play Console - show demo purchase
                Toast.makeText(this, "Demo mode: Product not configured in Play Console", Toast.LENGTH_LONG).show()
                // For testing: grant premium manually
                val type = if (productId.contains("goodplan", ignoreCase = true)) "GOODPLAN" else "BASIC"
                repository.isPremium = true
                repository.premiumType = type
                updatePremiumStatus()
            }
        } catch (e: Exception) {
            Log.e("PremiumActivity", "Error purchasing", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePremiumStatus() {
        try {
            if (repository.isPremium) {
                binding.tvCurrentPlan.text = "${getString(R.string.premium)}: ${repository.premiumType}"
                binding.tvCurrentPlan.visibility = android.view.View.VISIBLE
            } else {
                binding.tvCurrentPlan.visibility = android.view.View.GONE
            }
        } catch (e: Exception) {
            Log.e("PremiumActivity", "Error updating premium status", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            billingManager?.endConnection()
        } catch (e: Exception) {
            Log.e("PremiumActivity", "Error ending billing connection", e)
        }
    }
}
