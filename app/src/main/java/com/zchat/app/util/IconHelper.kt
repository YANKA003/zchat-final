package com.zchat.app.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Helper class to manage dynamic app icon based on selected theme
 */
object IconHelper {
    private const val TAG = "IconHelper"

    // Icon aliases - must match AndroidManifest.xml
    private const val ALIAS_DEFAULT = "com.zchat.app.ui.auth.AuthActivityDefault"
    private const val ALIAS_CLASSIC = "com.zchat.app.ui.auth.AuthActivityClassic"
    private const val ALIAS_MODERN = "com.zchat.app.ui.auth.AuthActivityModern"
    private const val ALIAS_NEON = "com.zchat.app.ui.auth.AuthActivityNeon"
    private const val ALIAS_CHILDISH = "com.zchat.app.ui.auth.AuthActivityChildish"

    // Theme constants
    const val THEME_DEFAULT = 0
    const val THEME_CLASSIC = 1
    const val THEME_MODERN = 2
    const val THEME_NEON = 3
    const val THEME_CHILDISH = 4

    /**
     * Update app icon based on theme
     * @param context Application context
     * @param theme Theme index (0 = default, 1 = classic, 2 = modern, 3 = neon, 4 = childish)
     */
    fun updateIcon(context: Context, theme: Int) {
        try {
            val packageManager = context.packageManager

            // All icon aliases
            val aliases = listOf(
                ALIAS_DEFAULT,
                ALIAS_CLASSIC,
                ALIAS_MODERN,
                ALIAS_NEON,
                ALIAS_CHILDISH
            )

            // Determine which alias to enable based on theme
            // For theme 0 (classic style in settings), use default icon
            // For theme 1 (modern style), use modern icon
            // etc.
            val enabledAlias = when (theme) {
                0 -> ALIAS_DEFAULT  // Classic theme -> default icon
                1 -> ALIAS_MODERN   // Modern theme -> modern icon
                2 -> ALIAS_NEON     // Neon theme -> neon icon
                3 -> ALIAS_CHILDISH // Childish theme -> childish icon
                else -> ALIAS_DEFAULT
            }

            Log.d(TAG, "Setting icon for theme $theme: $enabledAlias")

            // Disable all aliases except the selected one
            for (alias in aliases) {
                val state = if (alias == enabledAlias) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

                packageManager.setComponentEnabledSetting(
                    ComponentName(context, alias),
                    state,
                    PackageManager.DONT_KILL_APP
                )
            }

            Log.d(TAG, "Icon updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating icon", e)
        }
    }

    /**
     * Get the current enabled icon alias
     */
    fun getCurrentIconAlias(context: Context): String {
        val packageManager = context.packageManager
        val aliases = listOf(
            ALIAS_DEFAULT to THEME_DEFAULT,
            ALIAS_CLASSIC to THEME_CLASSIC,
            ALIAS_MODERN to THEME_MODERN,
            ALIAS_NEON to THEME_NEON,
            ALIAS_CHILDISH to THEME_CHILDISH
        )

        for ((alias, theme) in aliases) {
            val state = packageManager.getComponentEnabledSetting(
                ComponentName(context, alias)
            )
            if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                return alias
            }
        }

        return ALIAS_DEFAULT
    }

    /**
     * Get theme index from current icon
     */
    fun getCurrentThemeFromIcon(context: Context): Int {
        val currentAlias = getCurrentIconAlias(context)
        return when (currentAlias) {
            ALIAS_DEFAULT -> 0
            ALIAS_CLASSIC -> 1
            ALIAS_MODERN -> 2
            ALIAS_NEON -> 3
            ALIAS_CHILDISH -> 4
            else -> 0
        }
    }
}
