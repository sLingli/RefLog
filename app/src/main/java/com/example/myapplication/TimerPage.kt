package com.example.myapplication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 计时器页面 - Compose 版本
 *
 * 对齐原 page_timer.xml 布局：
 * - 主计时器卡片（状态标签 + 主时间 + 补时胶囊）
 * - 控制按钮区域（主按钮 + 结束半场按钮）
 *
 * 状态定义：
 *   STATE_READY    → "start"  模式：显示开始按钮
 *   STATE_RUNNING  → "pause"  模式：显示暂停 + 结束半场按钮
 *   STATE_PAUSED   → "resume" 模式：显示继续 + 结束半场按钮
 *   STATE_HALFTIME → "halftime" 模式：显示开始下半场按钮
 *   STATE_FINISHED → "finished" 模式：显示重置按钮
 */

// 状态常量（与 MainActivity 保持一致）
const val TIMER_STATE_READY = "ready"
const val TIMER_STATE_RUNNING = "running"
const val TIMER_STATE_PAUSED = "paused"
const val TIMER_STATE_HALFTIME = "halftime"
const val TIMER_STATE_FINISHED = "finished"

const val HALF_FIRST_CODE = "code_first_half"
const val HALF_BREAK_CODE = "code_halftime"
const val HALF_SECOND_CODE = "code_second_half"

@Composable
fun TimerPage(
    // 状态
    state: String,
    currentHalf: String,
    statusText: String,
    statusColor: Color,
    statusIconRes: Int,
    mainTimeText: String,
    mainTimeColor: Color,
    stoppageTimeText: String,
    stoppageActive: Boolean,
    showEndHalfButton: Boolean,
    // 回调
    onMainButtonClick: () -> Unit,
    onEndHalfButtonLongPress: () -> Unit,
) {
    val scrollState = rememberScrollState()

    // 主按钮样式
    val (mainButtonText, mainButtonIconRes, mainButtonColor) = when (state) {
        TIMER_STATE_READY -> Triple(
            stringResource(R.string.btn_start),
            R.drawable.baseline_play_arrow_24,
            Color(0xFF2E7D32)
        )
        TIMER_STATE_RUNNING -> Triple(
            stringResource(R.string.btn_pause),
            R.drawable.pause_circle,
            Color(0xFFC62828)
        )
        TIMER_STATE_PAUSED -> Triple(
            stringResource(R.string.btn_resume),
            R.drawable.baseline_play_arrow_24,
            Color(0xFF2E7D32)
        )
        TIMER_STATE_HALFTIME -> Triple(
            stringResource(R.string.status_second_half),
            R.drawable.baseline_play_arrow_24,
            Color(0xFF2E7D32)
        )
        TIMER_STATE_FINISHED -> Triple(
            stringResource(R.string.btn_reset),
            R.drawable.ic_substitute,
            Color(0xFFC62828)
        )
        else -> Triple(
            stringResource(R.string.btn_start),
            R.drawable.baseline_play_arrow_24,
            Color(0xFF2E7D32)
        )
    }

    // 补时颜色
    val stoppageColor = if (stoppageActive) Color(0xFFFF6600) else Color(0xFF666666)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== 主计时器卡片 ==========
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 状态标签（带图标）
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    if (statusIconRes != 0) {
                        Icon(
                            painter = painterResource(id = statusIconRes),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 主时间显示
                Text(
                    text = mainTimeText,
                    color = mainTimeColor,
                    fontSize = 56.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // 补时胶囊
                Surface(
                    modifier = Modifier.padding(top = 8.dp),
                    shape = RoundedCornerShape(50),
                    color = stoppageColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.more_time),
                            contentDescription = null,
                            tint = stoppageColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stoppageTimeText,
                            color = stoppageColor,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ========== 空的补时卡片（与原布局保持一致占位） ==========
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
        ) {
            // 原布局中此卡片为空
        }

        // ========== 控制按钮区域 ==========
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 主控制按钮
            Button(
                onClick = onMainButtonClick,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mainButtonColor,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    painter = painterResource(id = mainButtonIconRes),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = mainButtonText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 结束半场按钮（带长按进度动画）
            if (showEndHalfButton) {
                EndHalfButton(
                    onLongPress = onEndHalfButtonLongPress,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 结束半场按钮 - 带长按进度动画
 *
 * 对齐原 XML 中 endHalfButton 的长按交互：
 * - 长按 1500ms 触发
 * - 按住时有进度动画（background.level 从 0 到 10000）
 * - 触觉反馈
 */
@Composable
private fun EndHalfButton(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }

    // 按住时播放进度动画
    LaunchedEffect(isPressed) {
        if (isPressed) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1500)
            )
            // 动画完成 = 长按成功
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onLongPress()
            isPressed = false
        } else {
            if (progress.isRunning) progress.stop()
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 200)
            )
        }
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 进度背景
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction = progress.value)
                .background(
                    Color(0xFFC62828).copy(alpha = 0.15f)
                )
        )

        // 按钮内容
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.stop_circle),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.btn_stop),
                color = contentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
