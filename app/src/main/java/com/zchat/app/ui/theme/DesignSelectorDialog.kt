package com.zchat.app.ui.theme

import android.app.Dialog
import android.content.Context
import android.view.Window
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.graphics.toColorInt
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
            val rbDesign1 = findViewById<RadioButton>(R.id.rbDesign1)
            val rbDesign2 = findViewById<RadioButton>(R.id.rbDesign2)
            
            // Установка текущего выбора
            selectedDesign = ThemeManager.getDesign()
            rbDesign1.isChecked = selectedDesign == ThemeManager.DESIGN_CLASSIC
            rbDesign2.isChecked = selectedDesign == ThemeManager.DESIGN_MODERN
            
            // Подсветка выбранной карточки
            updateCardSelection(cardDesign1, cardDesign2)
            
            // Обработка нажатий
            cardDesign1.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_CLASSIC, rbDesign1, rbDesign2, cardDesign1, cardDesign2)
            }
            
            cardDesign2.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_MODERN, rbDesign1, rbDesign2, cardDesign1, cardDesign2)
            }
            
            rbDesign1.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_CLASSIC, rbDesign1, rbDesign2, cardDesign1, cardDesign2)
            }
            
            rbDesign2.setOnClickListener {
                selectDesign(ThemeManager.DESIGN_MODERN, rbDesign1, rbDesign2, cardDesign1, cardDesign2)
            }
            
            setCancelable(true)
        }
        
        dialog?.show()
    }
    
    private fun selectDesign(
        design: Int, 
        rb1: RadioButton, 
        rb2: RadioButton,
        card1: CardView,
        card2: CardView
    ) {
        selectedDesign = design
        rb1.isChecked = design == ThemeManager.DESIGN_CLASSIC
        rb2.isChecked = design == ThemeManager.DESIGN_MODERN
        updateCardSelection(card1, card2)
        applyDesign()
    }
    
    private fun updateCardSelection(card1: CardView, card2: CardView) {
        val selectedStroke = "#6366F1".toColorInt()
        val unselectedStroke = "#E2E8F0".toColorInt()
        
        if (selectedDesign == ThemeManager.DESIGN_CLASSIC) {
            card1.strokeColor = selectedStroke
            card1.strokeWidth = 3
            card2.strokeColor = unselectedStroke
            card2.strokeWidth = 1
        } else {
            card1.strokeColor = unselectedStroke
            card1.strokeWidth = 1
            card2.strokeColor = "#3B82F6".toColorInt()
            card2.strokeWidth = 3
        }
    }
    
    private fun applyDesign() {
        if (selectedDesign != ThemeManager.getDesign()) {
            ThemeManager.setDesign(selectedDesign)
            val designName = if (selectedDesign == ThemeManager.DESIGN_CLASSIC) "Классический" else "Современный"
            Toast.makeText(context, "Выбран $designName дизайн", Toast.LENGTH_SHORT).show()
            onDesignSelected?.invoke(selectedDesign)
        }
        dialog?.dismiss()
    }
    
    fun dismiss() {
        dialog?.dismiss()
    }
}
