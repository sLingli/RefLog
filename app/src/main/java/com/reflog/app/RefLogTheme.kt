package com.reflog.app

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
 * 自定义应用颜色（语义色彩，不受 theme 影响但根据深浅模式微调）
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
 *
 * 支持：
 * 1. 外观模式：跟随系统 / 浅色 / 深色
 * 2. 动态取色：Android 12+ 壁纸取色 (Material You)，关闭时使用品牌绿
 */
@Composable
fun RefLogTheme(
    config: ThemeConfig = ThemeManager.config,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val isDarkMode = ThemeManager.isDarkMode(systemDark)

    // 确定 colorScheme
    val colorScheme = when {
        // 动态取色 ON + Android 12+
        config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isDarkMode ->
            dynamicDarkColorScheme(context)
        config.useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isDarkMode ->
            dynamicLightColorScheme(context)
        // 非动态取色 或 旧 Android → 品牌色
        isDarkMode -> BrandDarkScheme
        else -> BrandLightScheme
    }

    // 根据深浅模式微调语义颜色
    val appColors = if (isDarkMode) {
        AppColors()
    } else {
        AppColors(
            dateText = Color(0xFF2E7D32),
            stoppageText = Color(0xFFE65100),
            timerNormal = Color(0xFF2E7D32),
        )
    }

    // 动画化所有颜色，实现丝滑过渡
    val animatedScheme = animateColorScheme(colorScheme)
    val animatedAppColors = animateAppColors(appColors)

    // 同步状态栏/导航栏颜色
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
