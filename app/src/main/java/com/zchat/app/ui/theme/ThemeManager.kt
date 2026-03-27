package com.zchat.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Менеджер тем для переключения между дизайнами ZChat.
 * Поддерживает два стиля оформления:
 * - Дизайн 1: Классический (индиго/фиолетовый)
 * - Дизайн 2: Современный (синий/розовый)
 */
object ThemeManager {
    
    const val DESIGN_CLASSIC = 1
    const val DESIGN_MODERN = 2
    
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_DESIGN = "selected_design"
    
    private val _currentDesign = MutableStateFlow(DESIGN_CLASSIC)
    val currentDesign: StateFlow<Int> = _currentDesign
    
    private lateinit var prefs: SharedPreferences
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _currentDesign.value = prefs.getInt(KEY_DESIGN, DESIGN_CLASSIC)
    }
    
    fun setDesign(design: Int) {
        prefs.edit().putInt(KEY_DESIGN, design).apply()
        _currentDesign.value = design
    }
    
    fun getDesign(): Int = _currentDesign.value
    
    fun isClassicDesign(): Boolean = _currentDesign.value == DESIGN_CLASSIC
    
    fun isModernDesign(): Boolean = _currentDesign.value == DESIGN_MODERN
    
    /**
     * Возвращает цвета для текущего дизайна
     */
    fun getColors(): ThemeColors {
        return if (isClassicDesign()) getClassicColors() else getModernColors()
    }
    
    private fun getClassicColors(): ThemeColors {
        return ThemeColors(
            primary = "#6366F1",           // Индиго
            primaryDark = "#4F46E5",
            primaryLight = "#818CF8",
            background = "#F8FAFC",
            surface = "#FFFFFF",
            sentMessage = "#6366F1",       // Индиго
            sentMessageText = "#FFFFFF",
            receivedMessage = "#E2E8F0",   // Светло-серый
            receivedMessageText = "#1E293B",
            accent = "#6366F1",
            onlineIndicator = "#22C55E",
            textPrimary = "#1E293B",
            textSecondary = "#64748B",
            divider = "#E2E8F0",
            fabBackground = "#6366F1"
        )
    }
    
    private fun getModernColors(): ThemeColors {
        return ThemeColors(
            primary = "#3B82F6",           // Синий (Blue 500)
            primaryDark = "#2563EB",
            primaryLight = "#60A5FA",
            background = "#F8FAFC",
            surface = "#FFFFFF",
            sentMessage = "#3B82F6",       // Синий
            sentMessageText = "#FFFFFF",
            receivedMessage = "#F1F5F9",   // Светло-серый (Slate 100)
            receivedMessageText = "#1E293B",
            accent = "#EC4899",            // Розовый (Pink 500)
            onlineIndicator = "#22C55E",
            textPrimary = "#1E293B",
            textSecondary = "#64748B",
            divider = "#E2E8F0",
            fabBackground = "#EC4899"      // Розовый FAB
        )
    }
}

/**
 * Класс с цветами темы
 */
data class ThemeColors(
    val primary: String,
    val primaryDark: String,
    val primaryLight: String,
    val background: String,
    val surface: String,
    val sentMessage: String,
    val sentMessageText: String,
    val receivedMessage: String,
    val receivedMessageText: String,
    val accent: String,
    val onlineIndicator: String,
    val textPrimary: String,
    val textSecondary: String,
    val divider: String,
    val fabBackground: String
)
