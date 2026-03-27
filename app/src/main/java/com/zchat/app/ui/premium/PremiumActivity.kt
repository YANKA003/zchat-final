package com.zchat.app.ui.premium

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.databinding.ActivityPremiumBinding

class PremiumActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPremiumBinding
    private lateinit var repository: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = Repository(applicationContext)

        setSupportActionBar(binding.toolbarPremium)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.premium)

        setupPlans()

        // Show current premium status
        updatePremiumStatus()
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
            purchasePremium("BASIC", "monthly", 2.0)
        }

        // BASIC forever button
        binding.btnBasicForever.setOnClickListener {
            purchasePremium("BASIC", "forever", 6.0)
        }

        // GOODPLAN monthly button
        binding.btnGoodplanMonthly.setOnClickListener {
            purchasePremium("GOODPLAN", "monthly", 5.0)
        }

        // GOODPLAN forever button
        binding.btnGoodplanForever.setOnClickListener {
            purchasePremium("GOODPLAN", "forever", 15.0)
        }

        // Restore purchase
        binding.btnRestore.setOnClickListener {
            Toast.makeText(this, "Checking purchases...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun purchasePremium(type: String, duration: String, price: Double) {
        // TODO: Implement actual payment (Google Play Billing)
        // For now, just set the premium status
        repository.isPremium = true
        repository.premiumType = type

        Toast.makeText(
            this,
            "${getString(R.string.success)}! $type ($duration)",
            Toast.LENGTH_LONG
        ).show()

        updatePremiumStatus()
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
}
