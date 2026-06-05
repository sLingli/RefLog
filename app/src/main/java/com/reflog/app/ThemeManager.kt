package com.reflog.app

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * 外观模式枚举
 */
enum class AppearanceMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

/**
 * 主题配置
 */
data class ThemeConfig(
    val appearanceMode: AppearanceMode = AppearanceMode.FOLLOW_SYSTEM,
    val useDynamicColor: Boolean = true,
)

/**
 * 主题管理器
 *
 * 管理 Material You 主题配置（外观模式 + 动态取色），持久化到 SharedPreferences。
 * 自动迁移旧的 AppTheme 枚举配置。
 */
object ThemeManager {
    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_APPEARANCE_MODE = "app_appearance_mode"
    private const val KEY_DYNAMIC_COLOR = "app_dynamic_color"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 当前主题配置
     */
    var config: ThemeConfig
        get() {
            val modeName = prefs.getString(KEY_APPEARANCE_MODE, AppearanceMode.FOLLOW_SYSTEM.name)
            val mode = try {
                AppearanceMode.valueOf(modeName ?: AppearanceMode.FOLLOW_SYSTEM.name)
            } catch (_: Exception) {
                AppearanceMode.FOLLOW_SYSTEM
            }
            val dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
            return ThemeConfig(appearanceMode = mode, useDynamicColor = dynamicColor)
        }
        set(value) {
            prefs.edit {
                putString(KEY_APPEARANCE_MODE, value.appearanceMode.name)
                putBoolean(KEY_DYNAMIC_COLOR, value.useDynamicColor)
            }
        }

    /**
     * 判断当前是否为深色模式
     *
     * @param systemDark 系统深色状态（由 isSystemInDarkTheme() 提供），
     *                    FOLLOW_SYSTEM 模式需要此参数。
     */
    fun isDarkMode(systemDark: Boolean): Boolean {
        return when (config.appearanceMode) {
            AppearanceMode.FOLLOW_SYSTEM -> systemDark
            AppearanceMode.LIGHT -> false
            AppearanceMode.DARK -> true
        }
    }
}
