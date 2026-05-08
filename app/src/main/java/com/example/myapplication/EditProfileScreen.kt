package com.example.myapplication

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

/**
 * 编辑资料全屏页面
 *
 * UI 结构：
 * - Scaffold + TopAppBar（标题"编辑资料"，左侧返回箭头）
 * - 居中大头像（AsyncImage + CircleShape），右下角叠加编辑图标
 * - OutlinedTextField 昵称输入（20字符限制 + 计数器）
 * - 底部"保存并返回"按钮
 *
 * 裁剪流程：
 * 点击头像 → PickVisualMedia → CropImageContract（圆形 1:1 裁剪）→ 暂存本地状态
 *
 * 保存逻辑：
 * 点击"保存并返回" → onSave(nickname, avatarUri) → onNavigateBack()
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    currentAvatarUri: Uri?,
    currentNickname: String,
    onNavigateBack: () -> Unit,
    onSave: (newNickname: String, newAvatarUri: Uri?) -> Unit,
) {
    // 本地暂存状态：仅在点击保存时才持久化
    var pendingNickname by remember { mutableStateOf(currentNickname) }
    var pendingAvatarUri by remember { mutableStateOf(currentAvatarUri) }
    var nicknameError by remember { mutableStateOf(false) }

    // 1. 裁剪器（先定义，不依赖 photoPicker）
    val cropImageLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        if (result.isSuccessful) {
            pendingAvatarUri = result.uriContent
        }
    }

    // 2. 相册选择器（回调中直接调用裁剪器）
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
            // 获取 URI 后立刻传递给裁剪器，强制 1:1 圆形裁剪
            cropImageLauncher.launch(
                CropImageContractOptions(
                    uri = sourceUri,
                    cropImageOptions = CropImageOptions().apply {
                        cropShape = CropImageView.CropShape.OVAL
                        aspectRatioX = 1
                        aspectRatioY = 1
                        fixAspectRatio = true
                    }
                )
            )
        }
    }

    // 统一的选图触发方法
    val launchPhotoPicker = {
        photoPicker.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.title_edit_profile),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // === 头像区域 ===
            Box(contentAlignment = Alignment.Center) {
                // 主头像（120dp 圆形）
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { launchPhotoPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    if (pendingAvatarUri != null) {
                        AsyncImage(
                            model = pendingAvatarUri,
                            contentDescription = stringResource(R.string.label_change_avatar),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 右下角编辑图标叠加层
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { launchPhotoPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.label_change_avatar),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 头像点击提示
            Text(
                text = stringResource(R.string.label_change_avatar),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // === 昵称输入 ===
            OutlinedTextField(
                value = pendingNickname,
                onValueChange = { newValue ->
                    if (newValue.length <= 20) {
                        pendingNickname = newValue
                        nicknameError = false
                    } else {
                        nicknameError = true
                    }
                },
                label = { Text(stringResource(R.string.nickname_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences
                ),
                isError = nicknameError,
                supportingText = {
                    Text(
                        text = "${pendingNickname.length}/20",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )

            // 昵称过长错误提示
            if (nicknameError) {
                Text(
                    text = stringResource(R.string.nickname_too_long),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            // 弹性占位，将按钮推到底部
            Spacer(modifier = Modifier.weight(1f))

            // === 保存并返回按钮 ===
            Button(
                onClick = {
                    onSave(pendingNickname, pendingAvatarUri)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.label_save_and_back),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EditProfileScreenPreview() {
    MaterialTheme {
        EditProfileScreen(
            currentAvatarUri = null,
            currentNickname = "RefLog",
            onNavigateBack = {},
            onSave = { _, _ -> }
        )
    }
}
