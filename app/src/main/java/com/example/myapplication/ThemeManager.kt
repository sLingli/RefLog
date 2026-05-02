package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
            AppTheme.DARK_GREEN -> darkGreenScheme
            AppTheme.OCEAN_BLUE -> oceanBlueScheme
            AppTheme.SUNSET_ORANGE -> sunsetOrangeScheme
            AppTheme.PURPLE_GALAXY -> purpleGalaxyScheme
            AppTheme.LIGHT_MODE -> lightModeScheme
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

    // 深绿色方案 (默认)
    private val darkGreenScheme = darkColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF1B5E20),
        onPrimaryContainer = Color(0xFFC8E6C9),
        secondary = Color(0xFFFF9800),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFFE65100),
        onSecondaryContainer = Color(0xFFFFE0B2),
        background = Color(0xFF121212),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF424242),
        onSurfaceVariant = Color(0xFFBDBDBD),
        error = Color(0xFFCF6679),
        onError = Color.Black,
        outline = Color(0xFF666666),
        outlineVariant = Color(0xFF333333),
        surfaceContainerHighest = Color(0xFF333333),
        surfaceContainerHigh = Color(0xFF2D2D2D),
        surfaceContainer = Color(0xFF1E1E1E),
        surfaceContainerLow = Color(0xFF181818),
    )

    // 海洋蓝方案
    private val oceanBlueScheme = darkColorScheme(
        primary = Color(0xFF00BCD4),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFF00838F),
        onPrimaryContainer = Color(0xFFB2EBF2),
        secondary = Color(0xFFFF9800),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFFE65100),
        onSecondaryContainer = Color(0xFFFFE0B2),
        background = Color(0xFF0D1B2A),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF1B2838),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF2D3E50),
        onSurfaceVariant = Color(0xFFBDBDBD),
        error = Color(0xFFCF6679),
        onError = Color.Black,
        outline = Color(0xFF4A6A8A),
        outlineVariant = Color(0xFF1E3A5F),
        surfaceContainerHighest = Color(0xFF2A3F55),
        surfaceContainerHigh = Color(0xFF243750),
        surfaceContainer = Color(0xFF1B2838),
        surfaceContainerLow = Color(0xFF152230)
    )

    // 日落橙方案
    private val sunsetOrangeScheme = darkColorScheme(
        primary = Color(0xFFFF9800),
        onPrimary = Color.Black,
        primaryContainer = Color(0xFFE65100),
        onPrimaryContainer = Color(0xFFFFE0B2),
        secondary = Color(0xFF4CAF50),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFF1B5E20),
        onSecondaryContainer = Color(0xFFC8E6C9),
        background = Color(0xFF1A1210),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF2A201C),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF3D302A),
        onSurfaceVariant = Color(0xFFBDBDBD),
        error = Color(0xFFCF6679),
        onError = Color.Black,
        outline = Color(0xFF6A5040),
        outlineVariant = Color(0xFF2A1E18),
        surfaceContainerHighest = Color(0xFF3D302A),
        surfaceContainerHigh = Color(0xFF342820),
        surfaceContainer = Color(0xFF2A201C),
        surfaceContainerLow = Color(0xFF201810)
    )

    // 星空紫方案
    private val purpleGalaxyScheme = darkColorScheme(
        primary = Color(0xFF9C27B0),
        onPrimary = Color.White,
        primaryContainer = Color(0xFF6A0080),
        onPrimaryContainer = Color(0xFFE1BEE7),
        secondary = Color(0xFFFF9800),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFFE65100),
        onSecondaryContainer = Color(0xFFFFE0B2),
        background = Color(0xFF1A1020),
        onBackground = Color(0xFFE0E0E0),
        surface = Color(0xFF251830),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF3A2848),
        onSurfaceVariant = Color(0xFFBDBDBD),
        error = Color(0xFFCF6679),
        onError = Color.Black,
        outline = Color(0xFF5A4070),
        outlineVariant = Color(0xFF2A1838),
        surfaceContainerHighest = Color(0xFF3A2848),
        surfaceContainerHigh = Color(0xFF302040),
        surfaceContainer = Color(0xFF251830),
        surfaceContainerLow = Color(0xFF1E1028)
    )

    // 浅色模式方案
    private val lightModeScheme = lightColorScheme(
        primary = Color(0xFF4CAF50),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFC8E6C9),
        onPrimaryContainer = Color(0xFF1B5E20),
        secondary = Color(0xFFFF9800),
        onSecondary = Color.Black,
        secondaryContainer = Color(0xFFFFE0B2),
        onSecondaryContainer = Color(0xFFE65100),
        background = Color(0xFFF8F8F8),
        onBackground = Color(0xFF1A1A1A),
        surface = Color.White,
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFFF0F0F0),
        onSurfaceVariant = Color(0xFF666666),
        error = Color(0xFFD32F2F),
        onError = Color.White,
        outline = Color(0xFFCCCCCC),
        outlineVariant = Color(0xFFE0E0E0),
        surfaceContainerHighest = Color(0xFFF5F5F5),
        surfaceContainerHigh = Color(0xFFF0F0F0),
        surfaceContainer = Color(0xFFFAFAFA),
        surfaceContainerLow = Color(0xFFFFFFFF)
    )
}