package com.zchat.app.ui.theme

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.RadioButton
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.zchat.app.R

/**
 * Диалог для выбора дизайна приложения с предпросмотром
 */
class DesignSelectorDialog(
    private val context: Context,
    private val onDesignSelected: ((Int) -> Unit)? = null
) {
    
    private var dialog: Dialog? = null
    private var selectedDesign = ThemeManager.getDesign()
    
    fun show() {
        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_design_selector)
            
            // Установка ширины диалога
            window?.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.92).toInt(),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            window?.setBackgroundDrawableResource(android.R.drawable.dialog_holo_light_frame)
            
            val cardDesign1 = findViewById<CardView>(R.id.cardDesign1)
            val cardDesign2 = findViewById<CardView>(R.id.cardDesign2)
            val cardDesign3 = findViewById<CardView>(R.id.cardDesign3)
            val cardDesign4 = findViewById<CardView>(R.id.cardDesign4)
            val rbDesign1 = findViewById<RadioButton>(R.id.rbDesign1)
            val rbDesign2 = findViewById<RadioButton>(R.id.rbDesign2)
            val rbDesign3 = findViewById<RadioButton>(R.id.rbDesign3)
            val rbDesign4 = findViewById<RadioButton>(R.id.rbDesign4)
            
            // Установка текущего выбора
            selectedDesign = ThemeManager.getDesign()
            rbDesign1.isChecked = selectedDesign == ThemeManager.DESIGN_CLASSIC
            rbDesign2.isChecked = selectedDesign == ThemeManager.DESIGN_MODERN
            rbDesign3.isChecked = selectedDesign == ThemeManager.DESIGN_NEON
            rbDesign4.isChecked = selectedDesign == ThemeManager.DESIGN_CHILD
            
            // Подсветка выбранной карточки через alpha
            updateCardSelection(cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            
            // Обработка нажатий
            cardDesign1.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_CLASSIC, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            cardDesign2.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_MODERN, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            cardDesign3.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_NEON, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            cardDesign4.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_CHILD, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            rbDesign1.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_CLASSIC, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            rbDesign2.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_MODERN, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            rbDesign3.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_NEON, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            rbDesign4.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_CHILD, rbDesign1, rbDesign2, rbDesign3, rbDesign4, cardDesign1, cardDesign2, cardDesign3, cardDesign4)
            }
            
            setCancelable(true)
        }
        
        dialog?.show()
    }
    
    private fun selectDesign(
        design: Int, 
        rb1: RadioButton, 
        rb2: RadioButton,
        rb3: RadioButton,
        rb4: RadioButton,
        card1: CardView,
        card2: CardView,
        card3: CardView,
        card4: CardView
    ) {
        selectedDesign = design
        rb1.isChecked = design == ThemeManager.DESIGN_CLASSIC
        rb2.isChecked = design == ThemeManager.DESIGN_MODERN
        rb3.isChecked = design == ThemeManager.DESIGN_NEON
        rb4.isChecked = design == ThemeManager.DESIGN_CHILD
        updateCardSelection(card1, card2, card3, card4)
        applyDesign()
    }
    
    private fun updateCardSelection(card1: CardView, card2: CardView, card3: CardView, card4: CardView) {
        // Используем alpha для визуального выделения
        card1.alpha = if (selectedDesign == ThemeManager.DESIGN_CLASSIC) 1.0f else 0.6f
        card2.alpha = if (selectedDesign == ThemeManager.DESIGN_MODERN) 1.0f else 0.6f
        card3.alpha = if (selectedDesign == ThemeManager.DESIGN_NEON) 1.0f else 0.6f
        card4.alpha = if (selectedDesign == ThemeManager.DESIGN_CHILD) 1.0f else 0.6f
        
        card1.cardElevation = if (selectedDesign == ThemeManager.DESIGN_CLASSIC) 8f else 2f
        card2.cardElevation = if (selectedDesign == ThemeManager.DESIGN_MODERN) 8f else 2f
        card3.cardElevation = if (selectedDesign == ThemeManager.DESIGN_NEON) 8f else 2f
        card4.cardElevation = if (selectedDesign == ThemeManager.DESIGN_CHILD) 8f else 2f
    }
    
    private fun applyDesign() {
        if (selectedDesign != ThemeManager.getDesign()) {
            ThemeManager.setDesign(selectedDesign)
            val designName = when (selectedDesign) {
                ThemeManager.DESIGN_MODERN -> "Современный"
                ThemeManager.DESIGN_NEON -> "Neon"
                ThemeManager.DESIGN_CHILD -> "Drawn by a child"
                else -> "Классический"
            }
            Toast.makeText(context, "Выбран $designName дизайн", Toast.LENGTH_SHORT).show()
            onDesignSelected?.invoke(selectedDesign)
        }
        dialog?.dismiss()
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
}
