package com.reflog.app

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        // 右上角编辑按钮
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onEditProfileClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.edit_pencil),
                    contentDescription = stringResource(R.string.title_edit_profile),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 头像区域
        ProfileAvatar(
            avatarUri = avatarUri,
            onClick = {}
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = nickname,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        MeMenuItem(
            iconRes = R.drawable.theme,
            title = stringResource(R.string.title_theme_settings),
            onClick = onThemeClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        MeMenuItem(
            iconRes = R.drawable.language,
            title = stringResource(R.string.title_language),
            onClick = onLanguageClick
        )

        Spacer(modifier = Modifier.height(8.dp))

        MeMenuItem(
            iconRes = R.drawable.ic_info,
            title = stringResource(R.string.title_about),
            onClick = onAboutClick
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfileAvatar(
    avatarUri: Uri?,
    onClick: () -> Unit
) {
    val context = LocalContext.current

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
        }
    }
}

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
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
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
                painter = painterResource(id = R.drawable.arrow_forward),
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