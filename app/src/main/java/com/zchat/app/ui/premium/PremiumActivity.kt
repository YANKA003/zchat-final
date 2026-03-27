package com.zchat.app.ui.premium

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
import com.zchat.app.R
import com.zchat.app.data.Repository
import com.zchat.app.data.model.PremiumPricing
import com.zchat.app.databinding.ActivityPremiumBinding
import com.zchat.app.ui.theme.ThemeManager
import kotlinx.coroutines.launch

class PremiumActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPremiumBinding
    private lateinit var repository: Repository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        applyThemeColors()
        
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = Repository(applicationContext)
        
        setupUI()
        loadCurrentSubscription()
    }
    
    private fun setupUI() {
        binding.toolbar.setNavigationOnClickListener { finish() }
        
        // BASIC buttons
        binding.btnBasicMonthly.setOnClickListener {
            showPurchaseDialog("BASIC", PremiumPricing.BASIC_MONTHLY, true)
        }
        
        binding.btnBasicLifetime.setOnClickListener {
            showPurchaseDialog("BASIC", PremiumPricing.BASIC_LIFETIME, false)
        }
        
        // GOODPLAN buttons
        binding.btnGoodplanMonthly.setOnClickListener {
            showPurchaseDialog("GOODPLAN", PremiumPricing.GOODPLAN_MONTHLY, true)
        }
        
        binding.btnGoodplanLifetime.setOnClickListener {
            showPurchaseDialog("GOODPLAN", PremiumPricing.GOODPLAN_LIFETIME, false)
        }
    }
    
    private fun loadCurrentSubscription() {
        val settings = repository.preferencesManager.settings.value
        val isPremium = settings.premiumEnabled
        val premiumType = settings.premiumType
        
        if (isPremium && premiumType.isNotEmpty()) {
            binding.cardCurrentPlan.visibility = android.view.View.VISIBLE
            binding.tvCurrentPlan.text = when (premiumType) {
                "BASIC" -> "✓ BASIC активен"
                "GOODPLAN" -> "✓ GOODPLAN активен ⭐"
                else -> "✓ Premium активен"
            }
        } else {
            binding.cardCurrentPlan.visibility = android.view.View.GONE
        }
    }
    
    private fun showPurchaseDialog(type: String, price: Double, isMonthly: Boolean) {
        val planName = if (type == "BASIC") "BASIC" else "GOODPLAN ⭐"
        val period = if (isMonthly) "месяц" else "навсегда"
        val message = "Оформить подписку $planName на $period?\n\nСтоимость: $$price USD"
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Подтверждение покупки")
            .setMessage(message)
            .setPositiveButton("Купить") { _, _ ->
                processPurchase(type, price, isMonthly)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    
    private fun processPurchase(type: String, price: Double, isMonthly: Boolean) {
        // Демо-покупка (в реальном приложении - интеграция с платежами)
        val settings = repository.preferencesManager.settings.value
        
        val newSettings = settings.copy(
            premiumEnabled = true,
            premiumType = type,
            hasStarBadge = type == "GOODPLAN"
        )
        
        repository.preferencesManager.updateSettings(newSettings)
        
        val planName = if (type == "GOODPLAN") "GOODPLAN ⭐" else "BASIC"
        Toast.makeText(
            this,
            "Подписка $planName активирована! Спасибо за покупку.",
            Toast.LENGTH_LONG
        ).show()
        
        loadCurrentSubscription()
    }
    
    private fun applyThemeColors() {
        val colors = ThemeManager.getColors()
        window.statusBarColor = colors.primaryDark.toColorInt()
    }
}
