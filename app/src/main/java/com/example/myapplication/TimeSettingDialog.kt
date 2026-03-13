package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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

// 颜色定义
private val DialogBackgroundColor = Color(0xFF424242)
private val ButtonBackgroundColor = Color(0xFF333333)
private val TimeValueColor = Color(0xFF4CAF50)
private val UnitTextColor = Color(0xFF888888)
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
            .background(DialogBackgroundColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 时间选择区域
        // Time Selection Area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            // 减少按钮 (Decrease Button)
            Button(
                onClick = {
                    if (selectedTime > 5) {
                        selectedTime -= 5
                    }
                },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBackgroundColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.remove),
                    contentDescription = "Decrease",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // 时间显示 (Time Display)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Text(
                    text = selectedTime.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = TimeValueColor
                )
                Text(
                    text = unitText,
                    fontSize = 14.sp,
                    color = UnitTextColor
                )
            }

            // 增加按钮 (Increase Button)
            Button(
                onClick = {
                    if (selectedTime < 45) {
                        selectedTime += 5
                    }
                },
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonBackgroundColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_add_24),
                    contentDescription = "Increase",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }

        // 按钮区域 (Button Area)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 取消按钮 (Cancel Button)
            Button(
                onClick = { onResult(TimeSettingResult.Cancelled) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CancelButtonColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.cancel_24dp_1f1f1f_fill0_wght400_grad0_opsz24),
                    contentDescription = "Cancel",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // 确认按钮 (Confirm Button)
            Button(
                onClick = { onResult(TimeSettingResult.Confirmed(selectedTime)) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ConfirmButtonColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_play_arrow_24),
                    contentDescription = "Confirm",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 预览
 * Preview
 */
@Preview(showBackground = true)
@Composable
fun TimeSettingDialogPreview() {
    TimeSettingContent(
        initialMinutes = 45,
        onResult = {}
    )
}

