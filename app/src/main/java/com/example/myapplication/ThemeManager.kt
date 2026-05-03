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
    FOLLOW_SYSTEM,
    DARK_GREEN,
    OCEAN_BLUE,
    SUNSET_ORANGE,
    PURPLE_GALAXY,
    LIGHT_GREEN;

    companion object {
        /** 智能主题：跟随系统 */
        val smartThemes = listOf(FOLLOW_SYSTEM)
        /** 自定义主题 */
        val customThemes = listOf(DARK_GREEN, OCEAN_BLUE, SUNSET_ORANGE, PURPLE_GALAXY, LIGHT_GREEN)
    }
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
        // 迁移旧的 LIGHT_MODE 到 LIGHT_GREEN
        val current = prefs.getString(KEY_THEME, AppTheme.DARK_GREEN.name)
        if (current == "LIGHT_MODE") {
            prefs.edit { putString(KEY_THEME, AppTheme.LIGHT_GREEN.name) }
        }
    }

    /**
     * 当前主题
     */
    var currentTheme: AppTheme
        get() {
            val themeName = prefs.getString(KEY_THEME, AppTheme.FOLLOW_SYSTEM.name)
            return try {
                AppTheme.valueOf(themeName ?: AppTheme.FOLLOW_SYSTEM.name)
            } catch (_: Exception) {
                AppTheme.FOLLOW_SYSTEM
            }
        }
        set(value) {
            prefs.edit { putString(KEY_THEME, value.name) }
        }

    /**
     * 判断是否为跟随系统主题
     */
    fun isFollowSystem(theme: AppTheme = currentTheme): Boolean {
        return theme == AppTheme.FOLLOW_SYSTEM
    }

    /**
     * 获取 Material 色彩方案
     *
     * @param theme 主题模式
     * @param isDark 是否深色模式（仅跟随系统模式使用）
     */
    fun getColorScheme(theme: AppTheme = currentTheme, isDark: Boolean = true): ColorScheme {
        return when (theme) {
            AppTheme.FOLLOW_SYSTEM -> if (isDark) DefaultDarkScheme else DefaultLightScheme
            AppTheme.DARK_GREEN -> DarkGreenScheme
            AppTheme.OCEAN_BLUE -> OceanBlueScheme
            AppTheme.SUNSET_ORANGE -> SunsetOrangeScheme
            AppTheme.PURPLE_GALAXY -> PurpleGalaxyScheme
            AppTheme.LIGHT_GREEN -> LightGreenScheme
        }
    }

    /**
     * 获取主题名称资源 ID
     */
    fun getThemeNameResId(theme: AppTheme): Int {
        return when (theme) {
            AppTheme.FOLLOW_SYSTEM -> R.string.theme_follow_system
            AppTheme.DARK_GREEN -> R.string.theme_dark_green
            AppTheme.OCEAN_BLUE -> R.string.theme_ocean_blue
            AppTheme.SUNSET_ORANGE -> R.string.theme_sunset_orange
            AppTheme.PURPLE_GALAXY -> R.string.theme_purple_galaxy
            AppTheme.LIGHT_GREEN -> R.string.theme_light_green
        }
    }

    /**
     * 获取主题描述资源 ID（智能主题才有）
     */
    fun getThemeDescResId(theme: AppTheme): Int? {
        return when (theme) {
            AppTheme.FOLLOW_SYSTEM -> R.string.label_follow_system_desc
            else -> null
        }
    }

    /**
     * 获取主题预览色（主色）
     */
    fun getThemePrimaryColor(theme: AppTheme): Color {
        return when (theme) {
            AppTheme.FOLLOW_SYSTEM -> Color(0xFF4CAF50)
            AppTheme.DARK_GREEN -> Color(0xFF4CAF50)
            AppTheme.OCEAN_BLUE -> Color(0xFF00BCD4)
            AppTheme.SUNSET_ORANGE -> Color(0xFFFF9800)
            AppTheme.PURPLE_GALAXY -> Color(0xFF9C27B0)
            AppTheme.LIGHT_GREEN -> Color(0xFF4CAF50)
        }
    }

    /**
     * 获取主题背景色
     */
    fun getThemeBackgroundColor(theme: AppTheme): Color {
        return when (theme) {
            AppTheme.FOLLOW_SYSTEM -> Color(0xFF121212)
            AppTheme.DARK_GREEN -> Color(0xFF121212)
            AppTheme.OCEAN_BLUE -> Color(0xFF0D1B2A)
            AppTheme.SUNSET_ORANGE -> Color(0xFF1A1210)
            AppTheme.PURPLE_GALAXY -> Color(0xFF1A1020)
            AppTheme.LIGHT_GREEN -> Color(0xFFF8F8F8)
        }
    }
}
