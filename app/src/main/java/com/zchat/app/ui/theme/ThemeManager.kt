package com.zchat.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Менеджер тем для переключения между дизайнами GOODOK.
 * Поддерживает четыре стиля оформления:
 * - Дизайн 1: Классический (индиго/фиолетовый)
 * - Дизайн 2: Современный (синий/розовый)
 * - Дизайн 3: Neon (фиолетово-синий градиент)
 * - Дизайн 4: Drawn by a child (детский рисунок)
 */
object ThemeManager {
    
    const val DESIGN_CLASSIC = 1
    const val DESIGN_MODERN = 2
    const val DESIGN_NEON = 3
    const val DESIGN_CHILD = 4
    
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
    
    fun isNeonDesign(): Boolean = _currentDesign.value == DESIGN_NEON
    
    fun isChildDesign(): Boolean = _currentDesign.value == DESIGN_CHILD
    
    /**
     * Возвращает цвета для текущего дизайна
     */
    fun getColors(): ThemeColors {
        return when (_currentDesign.value) {
            DESIGN_MODERN -> getModernColors()
            DESIGN_NEON -> getNeonColors()
            DESIGN_CHILD -> getChildColors()
            else -> getClassicColors()
        }
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
            fabBackground = "#6366F1",
            gradientStart = "#6366F1",
            gradientEnd = "#6366F1"
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
            fabBackground = "#EC4899",     // Розовый FAB
            gradientStart = "#3B82F6",
            gradientEnd = "#3B82F6"
        )
    }
    
    private fun getNeonColors(): ThemeColors {
        return ThemeColors(
            primary = "#6C5CE7",           // Фиолетовый Neon
            primaryDark = "#5B4BDB",
            primaryLight = "#A29BFE",
            background = "#F5F7FA",        // Светло-серый
            surface = "#FFFFFF",
            sentMessage = "#6C5CE7",       // Фиолетовый
            sentMessageText = "#FFFFFF",
            receivedMessage = "#FFFFFF",   // Белый
            receivedMessageText = "#2D3436",
            accent = "#A29BFE",            // Светло-фиолетовый
            onlineIndicator = "#00B894",
            textPrimary = "#2D3436",
            textSecondary = "#636E72",
            divider = "#DFE6E9",
            fabBackground = "#6C5CE7",
            gradientStart = "#6C5CE7",     // Градиент для профиля
            gradientEnd = "#A29BFE"
        )
    }
    
    private fun getChildColors(): ThemeColors {
        return ThemeColors(
            primary = "#5DADE2",           // Светло-голубой (light blue)
            primaryDark = "#3498DB",
            primaryLight = "#85C1E9",
            background = "#FEF9E7",        // Светло-желтоватый (как бумага для рисования)
            surface = "#FFFFFF",
            sentMessage = "#AF7AC5",       // Фиолетовый (как на рисунке)
            sentMessageText = "#FFFFFF",
            receivedMessage = "#FFFFFF",   // Белый с рамкой
            receivedMessageText = "#2C3E50",
            accent = "#F4D03F",            // Желтый акцент
            onlineIndicator = "#58D68D",   // Ярко-зеленый
            textPrimary = "#2C3E50",       // Темно-синий текст
            textSecondary = "#5D6D7E",
            divider = "#AED6F1",           // Светло-голубой
            fabBackground = "#F4D03F",     // Желтый FAB
            gradientStart = "#5DADE2",     // Голубой градиент
            gradientEnd = "#AF7AC5"        // Фиолетовый
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
    val fabBackground: String,
    val gradientStart: String,
    val gradientEnd: String
)
