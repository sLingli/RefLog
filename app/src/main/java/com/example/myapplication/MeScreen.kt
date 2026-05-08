package com.example.myapplication

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
 * - 自定义头像（点击从相册选择）
 * - 自定义昵称（点击编辑）
 * - 主题样式入口（ThemeSelectionDialog）
 * - 语言切换入口（LanguageSelectionDialog）
 * - 关于入口（AboutScreen）
 */
@Composable
fun MeScreenDialog(
    onDismiss: () -> Unit,
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    avatarUri: Uri? = null,
    nickname: String = stringResource(R.string.default_nickname),
    onEditProfileClick: () -> Unit = {},
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
            onLanguageClick = onLanguageClick,
            onSettingsClick = onSettingsClick,
            onAboutClick = onAboutClick,
            onDismiss = onDismiss,
            avatarUri = avatarUri,
            nickname = nickname,
            onEditProfileClick = onEditProfileClick,
        )
    }
}

@Composable
fun MeScreenContent(
    onThemeClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
    onDismiss: () -> Unit,
    avatarUri: Uri? = null,
    nickname: String = stringResource(R.string.default_nickname),
    onEditProfileClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 右上角编辑按钮 - 统一跳转编辑资料页面
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onEditProfileClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.title_edit_profile),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 头像区域 - 仅展示，不可点击（编辑统一走编辑页）
        ProfileAvatar(
            avatarUri = avatarUri,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 昵称区域 - 仅展示，不可点击（编辑统一走编辑页）
        Text(
            text = nickname,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 主题样式按钮
        MeMenuItem(
            iconRes = R.drawable.ic_palette,
            title = stringResource(R.string.title_theme),
            onClick = onThemeClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 语言切换按钮
        MeMenuItem(
            iconRes = R.drawable.language,
            title = stringResource(R.string.title_language),
            onClick = onLanguageClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 关于按钮
        MeMenuItem(
            iconRes = R.drawable.ic_info,
            title = stringResource(R.string.title_about),
            onClick = onAboutClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * 头像组件 - 显示用户自定义头像或默认图标
 */
@Composable
private fun ProfileAvatar(
    avatarUri: Uri?,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // 将 URI 加载为 ImageBitmap（避免引入 Coil 等额外依赖）
    val avatarBitmap: ImageBitmap? = remember(avatarUri) {
        avatarUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    Box(
        modifier = Modifier
            .padding(bottom = 4.dp)
            .size(80.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape = CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (avatarBitmap != null) {
            Image(
                bitmap = avatarBitmap,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_profile),
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 我的页面菜单项
 */
@Composable
fun MeMenuItem(
    iconRes: Int,
    title: String,
    subtitle: String = "",
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
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MeScreenDialogPreview() {
    MeScreenContent(
        onThemeClick = {},
        onLanguageClick = {},
        onSettingsClick = {},
        onAboutClick = {},
        onDismiss = {},
    )
}
