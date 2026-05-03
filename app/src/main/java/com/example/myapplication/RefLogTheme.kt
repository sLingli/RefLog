package com.example.myapplication

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
 * 所有 Compose UI 应使用此主题。
 * 状态栏/导航栏颜色由 SideEffect 自动同步，无需手动调用 applyThemeColors()。
 */
@Composable
fun RefLogTheme(
    theme: AppTheme = ThemeManager.currentTheme,
    content: @Composable () -> Unit,
) {
    val colorScheme = ThemeManager.getColorScheme(theme)

    // 根据主题微调语义颜色
    val appColors = when (theme) {
        AppTheme.LIGHT_MODE -> AppColors(
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
        else -> AppColors() // 默认值
    }

    // 同步状态栏/导航栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                theme == AppTheme.LIGHT_MODE
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
