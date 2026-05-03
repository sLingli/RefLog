package com.example.myapplication

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 自定义应用颜色（语义色彩，不受主题影响但可根据主题微调）
 * Custom app colors for semantic/event-specific colors
 */
@Immutable
data class AppColors(
    // 事件颜色 (语义色，保持不变)
    val goalColor: Color = Color(0xFF4CAF50),
    val yellowCardColor: Color = Color(0xFFFFEB3B),
    val redCardColor: Color = Color(0xFFF44336),
    val injuryColor: Color = Color(0xFF2196F3),
    val substitutionColor: Color = Color(0xFF9C27B0),

    // 计时器颜色
    val timerNormal: Color = Color(0xFF4CAF50),
    val timerWarning: Color = Color(0xFFFF9800),
    val timerDanger: Color = Color(0xFFF44336),
    val timerInactive: Color = Color(0xFF666666),

    // 日期文字颜色
    val dateText: Color = Color(0xFF4CAF50),
    // 补时文字颜色
    val stoppageText: Color = Color(0xFFFF9800),

    // 删除按钮颜色 (红色语义)
    val deleteColor: Color = Color(0xFFD32F2F),
    // 清空按钮颜色 (红色语义)
    val clearAllColor: Color = Color(0xFFFF3B30),
)

val LocalAppColors = staticCompositionLocalOf { AppColors() }

/**
 * RefLog 主题包装
 * RefLog Theme Wrapper
 *
 * 支持三种模式：
 * 1. 跟随系统 (FOLLOW_SYSTEM) — 根据系统深浅色自动切换
 * 2. 自定义固定主题 — 始终使用对应的 scheme
 *
 * 动态取色已集成到 FOLLOW_SYSTEM 中：Android 12+ 自动使用壁纸颜色。
 * 状态栏/导航栏颜色由 SideEffect 自动同步。
 */
@Composable
fun RefLogTheme(
    theme: AppTheme = ThemeManager.currentTheme,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    // 确定 colorScheme
    val colorScheme = when {
        // 跟随系统 + Android 12+ → 动态取色
        theme == AppTheme.FOLLOW_SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme ->
            dynamicDarkColorScheme(context)
        theme == AppTheme.FOLLOW_SYSTEM && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !darkTheme ->
            dynamicLightColorScheme(context)
        // 跟随系统 + 旧设备 → 使用默认深浅色方案
        theme == AppTheme.FOLLOW_SYSTEM ->
            ThemeManager.getColorScheme(AppTheme.FOLLOW_SYSTEM, isDark = darkTheme)
        // 其他固定主题 → 使用对应 scheme
        else -> ThemeManager.getColorScheme(theme)
    }

    // 判断实际是否深色模式（用于语义颜色和状态栏）
    val isDarkMode = when (theme) {
        AppTheme.FOLLOW_SYSTEM -> darkTheme
        AppTheme.LIGHT_GREEN -> false
        else -> true // DARK_GREEN, OCEAN_BLUE, SUNSET_ORANGE, PURPLE_GALAXY 都是暗色主题
    }

    // 根据主题微调语义颜色
    val appColors = when (theme) {
        AppTheme.FOLLOW_SYSTEM -> if (isDarkMode) {
            AppColors() // 深色默认
        } else {
            AppColors(
                dateText = Color(0xFF2E7D32),
                stoppageText = Color(0xFFE65100),
                timerNormal = Color(0xFF2E7D32),
            )
        }
        AppTheme.LIGHT_GREEN -> AppColors(
            dateText = Color(0xFF2E7D32),
            stoppageText = Color(0xFFE65100),
            timerNormal = Color(0xFF2E7D32),
        )
        AppTheme.OCEAN_BLUE -> AppColors(
            dateText = Color(0xFF00BCD4),
            stoppageText = Color(0xFFFF9800),
            timerNormal = Color(0xFF00BCD4),
        )
        AppTheme.SUNSET_ORANGE -> AppColors(
            dateText = Color(0xFFFF9800),
            stoppageText = Color(0xFF4CAF50),
            timerNormal = Color(0xFFFF9800),
        )
        AppTheme.PURPLE_GALAXY -> AppColors(
            dateText = Color(0xFF9C27B0),
            stoppageText = Color(0xFFFF9800),
            timerNormal = Color(0xFF9C27B0),
        )
        else -> AppColors()
    }

    // 同步状态栏/导航栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

/**
 * 便捷访问 AppColors
 */
object AppThemeColors {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}
