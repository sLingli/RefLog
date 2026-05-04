package com.example.myapplication

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
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

/** 动画时长 */
private const val THEME_ANIM_DURATION = 400

/**
 * 动画化 ColorScheme
 *
 * 将 ColorScheme 中所有颜色字段用 animateColorAsState(tween) 包裹，
 * 使主题切换时所有颜色平滑过渡而非瞬间跳变。
 */
@Composable
private fun animateColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = THEME_ANIM_DURATION, easing = FastOutSlowInEasing)
    return target.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, spec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(target.tertiary, spec, label = "tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, spec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(target.tertiaryContainer, spec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(target.onTertiaryContainer, spec, label = "onTertiaryContainer").value,
        background = animateColorAsState(target.background, spec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant").value,
        error = animateColorAsState(target.error, spec, label = "error").value,
        onError = animateColorAsState(target.onError, spec, label = "onError").value,
        errorContainer = animateColorAsState(target.errorContainer, spec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(target.onErrorContainer, spec, label = "onErrorContainer").value,
        outline = animateColorAsState(target.outline, spec, label = "outline").value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec, label = "outlineVariant").value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec, label = "surfaceContainerHighest").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec, label = "surfaceContainerHigh").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec, label = "surfaceContainer").value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec, label = "surfaceContainerLow").value,
        surfaceBright = animateColorAsState(target.surfaceBright, spec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(target.surfaceDim, spec, label = "surfaceDim").value,
        inverseSurface = animateColorAsState(target.inverseSurface, spec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(target.inverseOnSurface, spec, label = "inverseOnSurface").value,
        inversePrimary = animateColorAsState(target.inversePrimary, spec, label = "inversePrimary").value,
        scrim = animateColorAsState(target.scrim, spec, label = "scrim").value,
    )
}

/**
 * 动画化 AppColors
 *
 * 将语义色（事件色、计时器色等）同样做平滑过渡，保持全局视觉一致性。
 */
@Composable
private fun animateAppColors(target: AppColors): AppColors {
    val spec = tween<Color>(durationMillis = THEME_ANIM_DURATION, easing = FastOutSlowInEasing)
    return AppColors(
        goalColor = animateColorAsState(target.goalColor, spec, label = "goalColor").value,
        yellowCardColor = animateColorAsState(target.yellowCardColor, spec, label = "yellowCardColor").value,
        redCardColor = animateColorAsState(target.redCardColor, spec, label = "redCardColor").value,
        injuryColor = animateColorAsState(target.injuryColor, spec, label = "injuryColor").value,
        substitutionColor = animateColorAsState(target.substitutionColor, spec, label = "substitutionColor").value,
        timerNormal = animateColorAsState(target.timerNormal, spec, label = "timerNormal").value,
        timerWarning = animateColorAsState(target.timerWarning, spec, label = "timerWarning").value,
        timerDanger = animateColorAsState(target.timerDanger, spec, label = "timerDanger").value,
        timerInactive = animateColorAsState(target.timerInactive, spec, label = "timerInactive").value,
        dateText = animateColorAsState(target.dateText, spec, label = "dateText").value,
        stoppageText = animateColorAsState(target.stoppageText, spec, label = "stoppageText").value,
        deleteColor = animateColorAsState(target.deleteColor, spec, label = "deleteColor").value,
        clearAllColor = animateColorAsState(target.clearAllColor, spec, label = "clearAllColor").value,
    )
}

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

    // 动画化所有颜色，实现丝滑过渡
    val animatedScheme = animateColorScheme(colorScheme)
    val animatedAppColors = animateAppColors(appColors)

    // 同步状态栏/导航栏颜色（使用动画化后的颜色，消除渲染时差）
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedScheme.background.toArgb()
            window.navigationBarColor = animatedScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
        }
    }

    CompositionLocalProvider(LocalAppColors provides animatedAppColors) {
        MaterialTheme(
            colorScheme = animatedScheme,
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
