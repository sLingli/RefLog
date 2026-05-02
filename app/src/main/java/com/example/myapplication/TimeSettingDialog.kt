package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 时间设置弹窗
 * Time Setting Dialog
 *
 * 设置每半场比赛时间（5-45分钟）
 * Set match duration per half (5-45 minutes)
 */

// 语义颜色
private val CancelButtonColor = Color(0xFFFF3B30)
private val ConfirmButtonColor = Color(0xFF00E676)

/**
 * 时间设置结果
 * Time Setting Result
 */
sealed class TimeSettingResult {
    data class Confirmed(val minutes: Int) : TimeSettingResult()
    object Cancelled : TimeSettingResult()
}

/**
 * 时间设置弹窗
 * @param initialMinutes 初始时间（分钟）
 * @param onDismiss 关闭弹窗回调
 * @param onResult 结果回调
 */
@Composable
fun TimeSettingDialog(
    initialMinutes: Int = 45,
    onDismiss: () -> Unit,
    onResult: (TimeSettingResult) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        TimeSettingContent(
            initialMinutes = initialMinutes,
            onResult = { result ->
                onResult(result)
                onDismiss()
            }
        )
    }
}

/**
 * 弹窗内容
 * Dialog Content
 */
@Composable
fun TimeSettingContent(
    initialMinutes: Int = 45,
    onResult: (TimeSettingResult) -> Unit
) {
    var selectedTime by remember { mutableIntStateOf(initialMinutes) }
    val unitText = stringResource(R.string.unit_min_half)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 时间选择区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 减少按钮
            IconButton(
                onClick = { if (selectedTime > 5) selectedTime-- },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_close_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 时间显示
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = selectedTime.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = unitText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 增加按钮
            IconButton(
                onClick = { if (selectedTime < 45) selectedTime++ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_add_24),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 取消按钮
            Button(
                onClick = { onResult(TimeSettingResult.Cancelled) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CancelButtonColor
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_close_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            // 确认按钮
            Button(
                onClick = { onResult(TimeSettingResult.Confirmed(selectedTime)) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConfirmButtonColor
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_check_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TimeSettingDialogPreview() {
    TimeSettingContent(
        initialMinutes = 45,
        onResult = {}
    )
}
