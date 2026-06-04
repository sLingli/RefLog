package com.reflog.app

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 主题设置页面（全屏 NavHost 页面）
 *
 * 提供两个控制项：
 * 1. 外观模式：跟随系统 / 浅色模式 / 深色模式（3 个单选卡片）
 * 2. 动态取色：开关 + 描述（Android 12+ 可用）
 *
 * 所有修改通过 onConfigChanged 即时通知外层，实现实时主题切换。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onConfigChanged: ((ThemeConfig) -> Unit)? = null,
) {
    // 读取当前配置（从 ThemeManager 初始化，后续用本地 state 管理）
    var config by remember { mutableStateOf(ThemeManager.config) }

    fun apply(newConfig: ThemeConfig) {
        config = newConfig
        ThemeManager.config = newConfig
        onConfigChanged?.invoke(newConfig)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_theme_settings),
                        fontWeight = FontWeight.Bold,
                        fontSize = 35.sp
                    )
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ========== 外观模式 ==========
            SectionHeader(title = stringResource(R.string.appearance_mode))

            Spacer(modifier = Modifier.height(8.dp))

            AppearanceModeCard(
                mode = AppearanceMode.FOLLOW_SYSTEM,
                currentMode = config.appearanceMode,
                iconRes = R.drawable.sun_moon,
                title = stringResource(R.string.appearance_follow_system),
                subtitle = stringResource(R.string.appearance_follow_system_desc),
                onClick = { apply(config.copy(appearanceMode = AppearanceMode.FOLLOW_SYSTEM)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppearanceModeCard(
                mode = AppearanceMode.LIGHT,
                currentMode = config.appearanceMode,
                iconRes = R.drawable.ic_light_mode,
                title = stringResource(R.string.appearance_light),
                onClick = { apply(config.copy(appearanceMode = AppearanceMode.LIGHT)) }
            )

            Spacer(modifier = Modifier.height(8.dp))

            AppearanceModeCard(
                mode = AppearanceMode.DARK,
                currentMode = config.appearanceMode,
                iconRes = R.drawable.ic_dark_mode,
                title = stringResource(R.string.appearance_dark),
                onClick = { apply(config.copy(appearanceMode = AppearanceMode.DARK)) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ========== 动态取色 ==========
            SectionHeader(title = stringResource(R.string.dynamic_color))

            Spacer(modifier = Modifier.height(8.dp))

            DynamicColorCard(
                enabled = config.useDynamicColor,
                available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                onToggle = { apply(config.copy(useDynamicColor = it)) }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// region Sub-components

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

/**
 * 外观模式单选卡片
 */
@Composable
private fun AppearanceModeCard(
    mode: AppearanceMode,
    currentMode: AppearanceMode,
    iconRes: Int,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    val isSelected = mode == currentMode

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_check_24),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * 动态取色开关卡片
 */
@Composable
private fun DynamicColorCard(
    enabled: Boolean,
    available: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_palette),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (available) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.dynamic_color),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (available) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    }
                )
                Text(
                    text = if (available) {
                        stringResource(R.string.dynamic_color_desc)
                    } else {
                        stringResource(R.string.dynamic_color_unavailable)
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                enabled = available
            )
        }
    }
}

// endregion

@Preview(showBackground = true)
@Composable
fun ThemeSettingsScreenPreview() {
    MaterialTheme {
        ThemeSettingsScreen()
    }
}
