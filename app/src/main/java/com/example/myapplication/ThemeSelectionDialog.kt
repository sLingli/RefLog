package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 主题选择弹窗
 * Theme Selection Dialog
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme = ThemeManager.currentTheme,
    onDismiss: () -> Unit,
    onThemeSelected: (AppTheme) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        ThemeSelectionContent(
            currentTheme = currentTheme,
            onThemeSelected = { theme ->
                onThemeSelected(theme)
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun ThemeSelectionContent(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = stringResource(R.string.title_theme),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 主题网格 (2列)
        val themes = AppTheme.entries
        val rows = themes.chunked(2)

        rows.forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowThemes.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        isSelected = theme == currentTheme,
                        onClick = { onThemeSelected(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // 奇数时补一个空位
                if (rowThemes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 取消按钮
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Text(
                text = stringResource(R.string.btn_close),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 单个主题卡片
 */
@Composable
fun ThemeCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = ThemeManager.getThemePrimaryColor(theme)
    val bgColor = ThemeManager.getThemeBackgroundColor(theme)
    val themeName = stringResource(ThemeManager.getThemeNameResId(theme))

    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 颜色预览圆点 (三色)
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 背景色圆
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(bgColor, shape = CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            // 主色圆
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(primaryColor, shape = CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            // 背景色圆
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(bgColor, shape = CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 主题名称
        Text(
            text = themeName,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )

        // 选中标记
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                painter = androidx.compose.ui.res.painterResource(id = R.drawable.outline_check_24),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ThemeSelectionDialogPreview() {
    ThemeSelectionContent(
        currentTheme = AppTheme.DARK_GREEN,
        onThemeSelected = {},
        onDismiss = {}
    )
}