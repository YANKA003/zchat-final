package com.zchat.app.ui.theme

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.zchat.app.R

/**
 * Базовая активность с поддержкой смены дизайна.
 * Все активности должны наследоваться от этого класса для автоматического применения темы.
 */
abstract class BaseThemeActivity : AppCompatActivity() {
    
    protected lateinit var themeColors: ThemeColors
    
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)
        themeColors = ThemeManager.getColors()
        applyTheme()
        super.onCreate(savedInstanceState)
    }
    
    override fun onResume() {
        super.onResume()
        // Проверяем, не изменилась ли тема
        if (ThemeManager.getColors() != themeColors) {
            recreate()
        }
    }
    
    private fun applyTheme() {
        // Устанавливаем цвет status bar
        window.statusBarColor = themeColors.primaryDark.toColorInt()
    }
    
    /**
     * Применяет тему к тулбару
     */
    protected fun setupToolbar(toolbar: Toolbar) {
        toolbar.setBackgroundColor(themeColors.primary.toColorInt())
        toolbar.setTitleTextColor("#FFFFFF".toColorInt())
    }
    
    /**
     * Применяет фон для отправленного сообщения
     */
    protected fun applySentMessageBackground(view: View) {
        val drawable = GradientDrawable().apply {
            setColor(themeColors.sentMessage.toColorInt())
            cornerRadius = 32f
        }
        view.background = drawable
    }
    
    /**
     * Применяет фон для полученного сообщения
     */
    protected fun applyReceivedMessageBackground(view: View) {
        val drawable = GradientDrawable().apply {
            setColor(themeColors.receivedMessage.toColorInt())
            cornerRadius = 32f
        }
        view.background = drawable
    }
    
    /**
     * Применяет тему к кнопке отправки
     */
    protected fun applySendButton(button: ImageButton) {
        val drawable = GradientDrawable().apply {
            setColor(themeColors.primary.toColorInt())
            cornerRadius = 48f
        }
        button.background = drawable
    }
    
    /**
     * Применяет тему к FloatingActionButton
     */
    protected fun applyFabBackground(view: View) {
        val drawable = GradientDrawable().apply {
            setColor(themeColors.fabBackground.toColorInt())
            cornerRadius = 48f
        }
        view.background = drawable
    }
    
    /**
     * Возвращает цвет primary для текущей темы
     */
    protected fun getPrimaryColor(): Int = themeColors.primary.toColorInt()
    
    /**
     * Возвращает цвет текста отправленного сообщения
     */
    protected fun getSentTextColor(): Int = themeColors.sentMessageText.toColorInt()
    
    /**
     * Возвращает цвет текста полученного сообщения
     */
    protected fun getReceivedTextColor(): Int = themeColors.receivedMessageText.toColorInt()
}
