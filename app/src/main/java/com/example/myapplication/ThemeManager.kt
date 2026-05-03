package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 应用主题枚举
 * Application Theme Enum
 */
enum class AppTheme {
    DARK_GREEN,
    OCEAN_BLUE,
    SUNSET_ORANGE,
    PURPLE_GALAXY,
    LIGHT_MODE
}

/**
 * 主题管理器
 * Theme Manager
 *
 * 管理应用主题切换和持久化
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "app_theme"

    private lateinit var prefs: SharedPreferences

    /**
     * 初始化
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 当前主题
     */
    var currentTheme: AppTheme
        get() {
            val themeName = prefs.getString(KEY_THEME, AppTheme.DARK_GREEN.name)
            return try {
                AppTheme.valueOf(themeName ?: AppTheme.DARK_GREEN.name)
            } catch (_: Exception) {
                AppTheme.DARK_GREEN
            }
        }
        set(value) {
            prefs.edit { putString(KEY_THEME, value.name) }
        }

    /**
     * 获取 Material 色彩方案
     */
    fun getColorScheme(theme: AppTheme = currentTheme): ColorScheme {
        return when (theme) {
            AppTheme.DARK_GREEN -> DarkGreenScheme
            AppTheme.OCEAN_BLUE -> OceanBlueScheme
            AppTheme.SUNSET_ORANGE -> SunsetOrangeScheme
            AppTheme.PURPLE_GALAXY -> PurpleGalaxyScheme
            AppTheme.LIGHT_MODE -> LightModeScheme
        }
    }

    /**
     * 获取主题名称资源 ID
     */
    fun getThemeNameResId(theme: AppTheme): Int {
        return when (theme) {
            AppTheme.DARK_GREEN -> R.string.theme_dark_green
            AppTheme.OCEAN_BLUE -> R.string.theme_ocean_blue
            AppTheme.SUNSET_ORANGE -> R.string.theme_sunset_orange
            AppTheme.PURPLE_GALAXY -> R.string.theme_purple_galaxy
            AppTheme.LIGHT_MODE -> R.string.theme_light_mode
        }
    }

    /**
     * 获取主题预览色（主色）
     */
    fun getThemePrimaryColor(theme: AppTheme): Color {
        return when (theme) {
            AppTheme.DARK_GREEN -> Color(0xFF4CAF50)
            AppTheme.OCEAN_BLUE -> Color(0xFF00BCD4)
            AppTheme.SUNSET_ORANGE -> Color(0xFFFF9800)
            AppTheme.PURPLE_GALAXY -> Color(0xFF9C27B0)
            AppTheme.LIGHT_MODE -> Color(0xFF4CAF50)
        }
    }

    /**
     * 获取主题背景色
     */
    fun getThemeBackgroundColor(theme: AppTheme): Color {
        return when (theme) {
            AppTheme.DARK_GREEN -> Color(0xFF121212)
            AppTheme.OCEAN_BLUE -> Color(0xFF0D1B2A)
            AppTheme.SUNSET_ORANGE -> Color(0xFF1A1210)
            AppTheme.PURPLE_GALAXY -> Color(0xFF1A1020)
            AppTheme.LIGHT_MODE -> Color(0xFFF8F8F8)
        }
    }
}
