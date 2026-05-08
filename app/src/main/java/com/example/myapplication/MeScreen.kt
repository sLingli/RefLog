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
import androidx.compose.ui.text.style.TextAlign
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
    onAvatarClick: () -> Unit = {},
    onNicknameChange: (String) -> Unit = {},
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
            onAvatarClick = onAvatarClick,
            onNicknameChange = onNicknameChange,
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
    onAvatarClick: () -> Unit = {},
    onNicknameChange: (String) -> Unit = {},
) {
    var showEditNicknameDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 头像区域 - 点击可更换
        ProfileAvatar(
            avatarUri = avatarUri,
            onClick = onAvatarClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 昵称区域 - 点击可编辑
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { showEditNicknameDialog = true }
        ) {
            Text(
                text = nickname,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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

    // 编辑昵称弹窗
    if (showEditNicknameDialog) {
        EditNicknameDialog(
            currentNickname = nickname,
            onDismiss = { showEditNicknameDialog = false },
            onConfirm = { newNickname ->
                showEditNicknameDialog = false
                onNicknameChange(newNickname)
            }
        )
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
 * 编辑昵称弹窗
 */
@Composable
fun EditNicknameDialog(
    currentNickname: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(currentNickname) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.edit_nickname_title),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { newValue ->
                        if (newValue.length <= 20) {
                            text = newValue
                            isError = false
                        } else {
                            isError = true
                        }
                    },
                    label = { Text(stringResource(R.string.nickname_hint)) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(
                                text = stringResource(R.string.nickname_too_long),
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text(
                                text = "${text.length}/20",
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty()
            ) {
                Text(stringResource(R.string.nickname_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.nickname_cancel))
            }
        }
    )
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
