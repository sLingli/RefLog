package com.reflog.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
 *   STATE_HALFTIME → "halftime" 模式：显示开始下半场按钮（合并按钮）
 *   STATE_FINISHED → "finished" 模式：显示重置按钮（合并按钮）
 */

/**
 * 按钮布局模式
 * - TWO_BUTTONS: 主按钮 + 结束半场按钮并排
 * - SINGLE_MERGED: 仅主按钮，宽度填满（HALFTIME / FINISHED 状态）
 */
private enum class ButtonLayout { TWO_BUTTONS, SINGLE_MERGED }

@Composable
fun TimerPage(
    // 赛事信息
    matchName: String = "",
    homeTeamName: String = "",
    awayTeamName: String = "",
    homeTeamColor: Int = 0xFF1565C0.toInt(),
    awayTeamColor: Int = 0xFFC62828.toInt(),
    // 状态
    state: TimerState,
    currentHalf: HalfState,
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
        TimerState.READY -> Triple(
            stringResource(R.string.btn_start),
            R.drawable.baseline_play_arrow_24,
            MaterialTheme.colorScheme.primary
        )
        TimerState.RUNNING -> Triple(
            stringResource(R.string.btn_pause),
            R.drawable.pause_circle,
            Color(0xFFC62828)
        )
        TimerState.PAUSED -> Triple(
            stringResource(R.string.btn_resume),
            R.drawable.baseline_play_arrow_24,
            MaterialTheme.colorScheme.primary
        )
        TimerState.HALFTIME -> Triple(
            stringResource(R.string.status_second_half),
            R.drawable.baseline_play_arrow_24,
            MaterialTheme.colorScheme.primary
        )
        TimerState.FINISHED -> Triple(
            stringResource(R.string.btn_start),
            R.drawable.baseline_play_arrow_24,
            MaterialTheme.colorScheme.primary
        )
    }

    // 主按钮内容颜色（跟随主题）
    val mainButtonContentColor = when (state) {
        TimerState.RUNNING -> Color.White
        else -> MaterialTheme.colorScheme.onPrimary
    }

    // 补时颜色
    val stoppageColor = if (stoppageActive) Color(0xFFFF6600) else Color(0xFF666666)

    // 按钮布局模式
    val buttonLayout = when {
        // HALFTIME 和 FINISHED 状态：合并为单个按钮
        state == TimerState.HALFTIME -> ButtonLayout.SINGLE_MERGED
        // RUNNING 和 PAUSED 状态且显示结束按钮：两个按钮
        showEndHalfButton -> ButtonLayout.TWO_BUTTONS
        // READY 等其他状态：仅一个按钮，填满宽度
        else -> ButtonLayout.SINGLE_MERGED
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========== 赛事信息头部 ==========
        if (homeTeamName.isNotBlank() || awayTeamName.isNotBlank()) {
            MatchInfoHeader(
                matchName = matchName,
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                homeTeamColor = homeTeamColor,
                awayTeamColor = awayTeamColor,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

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

        // ========== 控制按钮区域（带动画切换） ==========
        AnimatedContent(
            targetState = buttonLayout,
            transitionSpec = {
                // 合并：旧内容向左滑出淡出，新内容从右侧滑入淡入
                // 拆分：旧内容向右滑出淡出，新内容从左侧滑入淡入
                val direction = if (targetState == ButtonLayout.SINGLE_MERGED) {
                    // TWO_BUTTONS → SINGLE_MERGED（合并）
                    slideInHorizontally(tween(300)) { it / 2 } + fadeIn(tween(300)) togetherWith
                        slideOutHorizontally(tween(300)) { -it / 2 } + fadeOut(tween(300))
                } else {
                    // SINGLE_MERGED → TWO_BUTTONS（拆分）
                    slideInHorizontally(tween(300)) { -it / 2 } + fadeIn(tween(300)) togetherWith
                        slideOutHorizontally(tween(300)) { it / 2 } + fadeOut(tween(300))
                }
                direction using SizeTransform(clip = false)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentKey = { it }
        ) { layout ->
            when (layout) {
                ButtonLayout.TWO_BUTTONS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 主控制按钮
                        MainControlButton(
                            text = mainButtonText,
                            iconRes = mainButtonIconRes,
                            color = mainButtonColor,
                            contentColor = mainButtonContentColor,
                            onClick = onMainButtonClick,
                            modifier = Modifier.weight(1f)
                        )

                        // 结束半场按钮（带长按进度动画）
                        if (showEndHalfButton) {
                            EndHalfButton(
                                onLongPress = onEndHalfButtonLongPress,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                ButtonLayout.SINGLE_MERGED -> {
                    MainControlButton(
                        text = mainButtonText,
                        iconRes = mainButtonIconRes,
                        color = mainButtonColor,
                        contentColor = mainButtonContentColor,
                        onClick = onMainButtonClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * 主控制按钮 - 提取为独立组件以便复用
 */
@Composable
private fun MainControlButton(
    text: String,
    iconRes: Int,
    color: Color,
    contentColor: Color = Color.White,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = contentColor
        )
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 结束半场按钮 - 带长按进度动画
 *
 * 交互说明：
 * - 长按 1500ms 触发结束
 * - 按住时红色进度条从左向右逐渐填满
 * - 松手时进度条向左消失（回退动画）
 * - 进度条颜色从浅红渐变到深红
 * - 触发成功时有触觉反馈
 */
@Composable
private fun EndHalfButton(
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    // 触发成功闪烁效果
    var flashTrigger by remember { mutableStateOf(false) }
    val flashAlpha = remember { Animatable(0f) }

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
            // 触发闪烁
            flashTrigger = true
            isPressed = false
        } else {
            if (progress.isRunning) progress.stop()
            progress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 300)
            )
        }
    }

    // 闪烁动画
    LaunchedEffect(flashTrigger) {
        if (flashTrigger) {
            flashAlpha.snapTo(0.4f)
            flashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 400)
            )
            flashTrigger = false
        }
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    // 进度条颜色：纯红
    val progressColor = Color(0xFFC62828)

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
    ) {
        // 进度背景 - 纯红色从左向右填满（底层）
        if (progress.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = progress.value)
                    .background(progressColor)
            )
        }

        // 触发成功闪烁叠加层
        if (flashAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFC62828).copy(alpha = flashAlpha.value))
            )
        }

        // 按钮内容（始终居中，浮在进度条之上）
        Row(
            modifier = Modifier.fillMaxSize(),
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

/**
 * 赛事信息头部 - 显示在计时器页面顶部
 *
 * 展示赛事名称和主客队信息（带颜色标识）
 */
@Composable
private fun MatchInfoHeader(
    matchName: String,
    homeTeamName: String,
    awayTeamName: String,
    homeTeamColor: Int,
    awayTeamColor: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 赛事名称
        if (matchName.isNotBlank()) {
            Text(
                text = matchName,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 主队 vs 客队
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 主队色块
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(homeTeamColor))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = homeTeamName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "  ${stringResource(R.string.label_vs)}  ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = awayTeamName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.width(8.dp))
            // 客队色块
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(awayTeamColor))
            )
        }
    }
}
