package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 我的页面弹窗
 * Me Screen Dialog
 *
 * 包含：
 * - 主题样式入口（ThemeSelectionDialog）
 * - 设置入口（设定主客队颜色）
 * - 关于入口（AboutScreen）
 */
@Composable
fun MeScreenDialog(
    onDismiss: () -> Unit,
    onThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        MeScreenContent(
            onThemeClick = onThemeClick,
            onSettingsClick = onSettingsClick,
            onAboutClick = onAboutClick,
            onDismiss = onDismiss
        )
    }
}

@Composable
fun MeScreenContent(
    onThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像区域
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_profile),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = stringResource(R.string.app_name),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 主题样式按钮
        MeMenuItem(
            iconRes = R.drawable.ic_palette,
            title = stringResource(R.string.title_theme),
            subtitle = stringResource(R.string.label_current_theme),
            onClick = onThemeClick
        )

        Spacer(modifier = Modifier.height(8.dp))


        // 关于按钮
        MeMenuItem(
            iconRes = R.drawable.ic_info,
            title = stringResource(R.string.title_about),
            subtitle = stringResource(R.string.app_name),
            onClick = onAboutClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 我的页面菜单项
 */
@Composable
fun MeMenuItem(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = ">",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Light
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MeScreenDialogPreview() {
    MeScreenContent(
        onThemeClick = {},
        onSettingsClick = {},
        onAboutClick = {},
        onDismiss = {},
    )
}
