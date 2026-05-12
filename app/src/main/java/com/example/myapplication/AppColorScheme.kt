package com.example.myapplication

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 品牌配色方案 — 动态取色关闭或 Android < 12 时使用
 */

// ==================== 品牌深色方案 ====================
val BrandDarkScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003A08),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF3E2700),
    secondaryContainer = Color(0xFFE65100),
    onSecondaryContainer = Color(0xFFFFE0B2),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFE53935),
    onError = Color(0xFF1A0000),
    outline = Color(0xFF666666),
    outlineVariant = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF333333),
    surfaceContainerHigh = Color(0xFF2D2D2D),
    surfaceContainer = Color(0xFF1E1E1E),
    surfaceContainerLow = Color(0xFF181818),
)

// ==================== 品牌浅色方案 ====================
val BrandLightScheme = lightColorScheme(
    primary = Color(0xFF2E7D32),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFE65100),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFFE65100),
    background = Color(0xFFFAFAFA),
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
    surfaceContainerLow = Color(0xFFFFFFFF),
)
