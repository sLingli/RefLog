package com.example.myapplication.ui

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.myapplication.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==================== 状态常量 ====================

const val MATCH_STATE_READY = "ready"
const val MATCH_STATE_RUNNING = "running"
const val MATCH_STATE_PAUSED = "paused"
const val MATCH_STATE_HALFTIME = "halftime"
const val MATCH_STATE_FINISHED = "finished"

const val HALF_FIRST = "code_first_half"
const val HALF_BREAK = "code_halftime"
const val HALF_SECOND = "code_second_half"

// ==================== 主页面 Compose ====================

/**
 * 主页面 Compose 版
 * 合并原 layout/activity_main.xml 和 layout-round/activity_main.xml
 * 自动适配圆形/方形屏幕
 */
@Composable
fun MainScreen(
    matchState: String,
    currentHalf: String,
    mainTimeFormatted: String,
    stoppageTimeFormatted: String,
    homeTeamColor: Color,
    awayTeamColor: Color,
    halfTimeSet: Int,  // 分钟
    onToggleTimer: () -> Unit,
    onEndHalf: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetMatchTime: () -> Unit,
    onHomeColorClick: () -> Unit,
    onAwayColorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRound = LocalConfiguration.current.isScreenRound

    // 屏幕形状自适应尺寸
    val clockTextSize = if (isRound) 30f else 28f
    val startBtnSize = if (isRound) 68.dp else 56.dp
    val fieldSize = if (isRound) 226.dp else 200.dp
    val statusTextSize = if (isRound) 16.sp else 14.sp
    val mainTimeTextSize = if (isRound) 64.sp else 56.sp
    val stoppageTextSize = if (isRound) 20.sp else 18.sp
    val controlBtnSize = if (isRound) 60.dp else 52.dp
    val controlPanelMarginBottom = if (isRound) 30.dp else 20.dp
    val historyBarWidth = if (isRound) 60.dp else 50.dp
    val historyBarMarginBottom = if (isRound) 12.dp else 6.dp
    val setTimeBtnHeight = if (isRound) 26.dp else 20.dp
    val setTimeBtnTextSize = if (isRound) 12.sp else 10.sp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (matchState) {
            MATCH_STATE_READY -> ReadyLayout(
                clockTextSize = clockTextSize,
                startBtnSize = startBtnSize,
                fieldSize = fieldSize,
                setTimeBtnHeight = setTimeBtnHeight,
                setTimeBtnTextSize = setTimeBtnTextSize,
                historyBarWidth = historyBarWidth,
                historyBarMarginBottom = historyBarMarginBottom,
                homeTeamColor = homeTeamColor,
                awayTeamColor = awayTeamColor,
                halfTimeSet = halfTimeSet,
                onStartClick = onToggleTimer,
                onSetMatchTime = onSetMatchTime,
                onHomeColorClick = onHomeColorClick,
                onAwayColorClick = onAwayColorClick,
                onOpenSettings = onOpenSettings
            )
            MATCH_STATE_RUNNING, MATCH_STATE_PAUSED,
            MATCH_STATE_HALFTIME, MATCH_STATE_FINISHED -> RunningLayout(
                matchState = matchState,
                currentHalf = currentHalf,
                mainTimeFormatted = mainTimeFormatted,
                stoppageTimeFormatted = stoppageTimeFormatted,
                statusTextSize = statusTextSize,
                mainTimeTextSize = mainTimeTextSize,
                stoppageTextSize = stoppageTextSize,
                controlBtnSize = controlBtnSize,
                controlPanelMarginBottom = controlPanelMarginBottom,
                onToggleTimer = onToggleTimer,
                onEndHalf = onEndHalf,
                onOpenSettings = onOpenSettings
            )
        }
    }
}

// ==================== 赛前界面 ====================

@Composable
private fun ReadyLayout(
    clockTextSize: Float,
    startBtnSize: Dp,
    fieldSize: Dp,
    setTimeBtnHeight: Dp,
    setTimeBtnTextSize: androidx.compose.ui.unit.TextUnit,
    historyBarWidth: Dp,
    historyBarMarginBottom: Dp,
    homeTeamColor: Color,
    awayTeamColor: Color,
    halfTimeSet: Int,
    onStartClick: () -> Unit,
    onSetMatchTime: () -> Unit,
    onHomeColorClick: () -> Unit,
    onAwayColorClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRound = LocalConfiguration.current.isScreenRound
    val clockMarginTop = if (isRound) 8.dp else 4.dp

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部实时时钟
            FluidClock(
                textSizeSp = clockTextSize,
                modifier = Modifier
                    .padding(top = clockMarginTop)
                    .height(40.dp)
                    .fillMaxWidth()
            )

            // 中央球场区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FieldWithStartButton(
                    fieldSize = fieldSize,
                    startBtnSize = startBtnSize,
                    homeTeamColor = homeTeamColor,
                    awayTeamColor = awayTeamColor,
                    onStartClick = onStartClick,
                    onHomeColorClick = onHomeColorClick,
                    onAwayColorClick = onAwayColorClick
                )
            }

            // 底部区域：时间设置按钮 + 小横条
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = historyBarMarginBottom)
            ) {
                // 比赛时间设置按钮
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF2A2A2A))
                        .clickable(onClick = onSetMatchTime)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .height(setTimeBtnHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.fmt_duration_simple, halfTimeSet),
                        color = Color.White,
                        fontSize = setTimeBtnTextSize
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 底部小横条（上滑触发设置）
                SwipeUpBar(
                    width = historyBarWidth,
                    onSwipeUp = onOpenSettings
                )
            }
        }
    }
}

/**
 * 球场 + 开始按钮 + 颜色覆盖层
 */
@Composable
private fun FieldWithStartButton(
    fieldSize: Dp,
    startBtnSize: Dp,
    homeTeamColor: Color,
    awayTeamColor: Color,
    onStartClick: () -> Unit,
    onHomeColorClick: () -> Unit,
    onAwayColorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRound = LocalConfiguration.current.isScreenRound

    Box(
        modifier = modifier.size(fieldSize),
        contentAlignment = Alignment.Center
    ) {
        // 球场图片
        Image(
            painter = painterResource(id = R.drawable.ic_football_field),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF888888)),
            modifier = Modifier.fillMaxSize()
        )

        // 主客队颜色覆盖层
        Row(modifier = Modifier.fillMaxSize()) {
            // 左半 - 主队
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(HalfLeftShape())
                    .background(homeTeamColor.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onHomeColorClick
                    )
            )
            // 右半 - 客队
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(HalfRightShape())
                    .background(awayTeamColor.copy(alpha = 0.4f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onAwayColorClick
                    )
            )
        }

        // 涟漪动画 + 开始按钮
        RippleStartButton(
            size = startBtnSize,
            onClick = onStartClick
        )
    }
}

/**
 * 带涟漪动画的开始按钮
 */
@Composable
private fun RippleStartButton(
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRound = LocalConfiguration.current.isScreenRound

    // 涟漪脉冲动画
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "ripple")
    val rippleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleScale"
    )
    val rippleAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rippleAlpha"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // 涟漪环
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = rippleScale
                    scaleY = rippleScale
                    alpha = rippleAlpha
                }
                .drawBehind {
                    drawCircle(
                        color = Color.White,
                        radius = this.size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                    )
                }
        )

        // 开始按钮
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF00CC66), Color(0xFF66FFB3))
                    )
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.btn_start),
                color = Color.Black,
                fontSize = if (isRound) 20.sp else 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 上滑小横条
 */
@Composable
private fun SwipeUpBar(
    width: Dp,
    onSwipeUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var startY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .width(width)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0xFF666666))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        startY = offset.y
                        val released = tryAwaitRelease()
                        // 点击也算触发
                        onSwipeUp()
                    }
                )
            }
    )
}

// ==================== 赛中界面 ====================

@Composable
private fun RunningLayout(
    matchState: String,
    currentHalf: String,
    mainTimeFormatted: String,
    stoppageTimeFormatted: String,
    statusTextSize: androidx.compose.ui.unit.TextUnit,
    mainTimeTextSize: androidx.compose.ui.unit.TextUnit,
    stoppageTextSize: androidx.compose.ui.unit.TextUnit,
    controlBtnSize: Dp,
    controlPanelMarginBottom: Dp,
    onToggleTimer: () -> Unit,
    onEndHalf: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 控制面板显示状态
    var controlPanelVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // 自动隐藏定时器
    fun showControlPanelWithAutoHide() {
        controlPanelVisible = true
        coroutineScope.launch {
            delay(3000)
            controlPanelVisible = false
        }
    }

    // 中场休息和比赛结束时强制显示控制面板
    LaunchedEffect(matchState) {
        if (matchState == MATCH_STATE_HALFTIME || matchState == MATCH_STATE_FINISHED) {
            controlPanelVisible = true
        } else if (matchState == MATCH_STATE_RUNNING) {
            // 开始运行后隐藏
            controlPanelVisible = false
        }
    }

    val controlPanelOffset by animateDpAsState(
        targetValue = if (controlPanelVisible) 0.dp else 200.dp,
        animationSpec = tween(300, easing = LinearOutSlowInEasing),
        label = "controlPanelOffset"
    )

    // 计时器文字颜色
    val timerColor = when {
        matchState == MATCH_STATE_HALFTIME || matchState == MATCH_STATE_FINISHED -> Color(0xFF888888)
        currentHalf == HALF_FIRST && mainTimeFormatted != "00:00" -> {
            // 上半场：检查是否超过半场时间（这里简化，具体逻辑在 Activity 中）
            colorResource(id = R.color.timer_normal)
        }
        else -> colorResource(id = R.color.timer_normal)
    }

    // 状态标签文字和图标
    val statusText = when (matchState) {
        MATCH_STATE_HALFTIME -> stringResource(R.string.status_halftime)
        MATCH_STATE_FINISHED -> stringResource(R.string.status_finished)
        else -> when (currentHalf) {
            HALF_FIRST -> stringResource(R.string.status_first_half)
            HALF_SECOND -> stringResource(R.string.status_second_half)
            else -> ""
        }
    }
    val statusIcon = when (matchState) {
        MATCH_STATE_HALFTIME -> R.drawable.ic_coffee
        MATCH_STATE_FINISHED -> R.drawable.ic_trophy
        else -> R.drawable.sports_soccer
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // 全屏触摸区域 - 点击显示控制面板
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { showControlPanelWithAutoHide() }
                )
        )

        // 中央计时器区域
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp) // 为控制面板留空间
        ) {
            // 状态标签
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp)
            ) {
                Icon(
                    painter = painterResource(id = statusIcon),
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = statusText,
                    color = Color(0xFF4CAF50),
                    fontSize = statusTextSize,
                    fontWeight = FontWeight.Bold
                )
            }

            // 主计时器
            Text(
                text = mainTimeFormatted,
                color = timerColor,
                fontSize = mainTimeTextSize,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(if (LocalConfiguration.current.isScreenRound) 4.dp else 2.dp))

            // 补时计时器
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50.dp))
                    .background(Color(0x1AFFFFFF))
                    .padding(horizontal = if (LocalConfiguration.current.isScreenRound) 12.dp else 10.dp, vertical = 3.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.more_time),
                    contentDescription = null,
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stoppageTimeFormatted,
                    color = Color(0xFF888888),
                    fontSize = stoppageTextSize
                )
            }
        }

        // 底部控制面板
        ControlPanel(
            matchState = matchState,
            buttonSize = controlBtnSize,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -controlPanelMarginBottom)
                .graphicsLayer { translationY = controlPanelOffset.toPx() },
            onToggleTimer = onToggleTimer,
            onEndHalf = onEndHalf
        )
    }
}

/**
 * 底部控制面板：暂停/继续 + 结束半场
 */
@Composable
private fun ControlPanel(
    matchState: String,
    buttonSize: Dp,
    modifier: Modifier = Modifier,
    onToggleTimer: () -> Unit,
    onEndHalf: () -> Unit
) {
    val isRound = LocalConfiguration.current.isScreenRound
    val panelPadding = if (isRound) 8.dp else 6.dp
    val btnMarginEnd = if (isRound) 16.dp else 12.dp
    val cornerRadius = (buttonSize / 2)

    // 暂停/继续按钮颜色动画
    val pauseBtnColor = when (matchState) {
        MATCH_STATE_RUNNING -> Color(0xFFD32F2F)  // 红色（暂停）
        MATCH_STATE_PAUSED -> Color(0xFF2E7D32)    // 绿色（继续）
        MATCH_STATE_HALFTIME -> Color(0xFF2E7D32)  // 绿色（开始下半场）
        MATCH_STATE_FINISHED -> Color(0xFFD32F2F)  // 红色（重置）
        else -> Color(0xFFD32F2F)
    }

    val pauseBtnIcon = when (matchState) {
        MATCH_STATE_RUNNING -> R.drawable.pause_circle
        MATCH_STATE_PAUSED -> R.drawable.baseline_play_arrow_24
        MATCH_STATE_HALFTIME -> R.drawable.baseline_play_arrow_24
        MATCH_STATE_FINISHED -> R.drawable.ic_substitute
        else -> R.drawable.baseline_play_arrow_24
    }

    // 是否显示结束按钮（中场休息和比赛结束时隐藏）
    val showEndButton = matchState == MATCH_STATE_RUNNING || matchState == MATCH_STATE_PAUSED

    // 半场/结束状态下按钮变长变居中
    val isWideMode = matchState == MATCH_STATE_HALFTIME || matchState == MATCH_STATE_FINISHED
    val actualButtonSize = if (isWideMode) 120.dp else buttonSize

    // 长按结束半场状态
    var isLongPressing by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    val holdProgressAnim = remember { Animatable(0f) }

    // 长按 1.5 秒后触发结束半场
    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            holdProgressAnim.snapTo(0f)
            holdProgressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(1500, easing = LinearEasing)
            ) {
                holdProgress = value
            }
            // 动画完成 → 触发结束半场
            onEndHalf()
            holdProgress = 0f
            isLongPressing = false
        } else {
            holdProgressAnim.snapTo(0f)
            holdProgress = 0f
        }
    }

    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC000000))
            .padding(panelPadding)
    ) {
        // 暂停/继续/下半场 按钮
        Box(
            modifier = Modifier
                .size(actualButtonSize, buttonSize)
                .clip(if (isWideMode) RoundedCornerShape(50) else CircleShape)
                .background(pauseBtnColor)
                .clickable(onClick = onToggleTimer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = pauseBtnIcon),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        if (showEndButton) {
            Spacer(modifier = Modifier.width(btnMarginEnd))

            // 结束半场按钮（长按 1.5 秒）
            Box(
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .drawBehind {
                        // 背景
                        drawCircle(color = Color(0xFF333333))
                        // 进度覆盖
                        if (holdProgress > 0f) {
                            drawArc(
                                color = Color(0xFFD32F2F),
                                startAngle = -90f,
                                sweepAngle = 360f * holdProgress,
                                useCenter = true
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isLongPressing = true
                                holdProgress = 0f
                                val released = tryAwaitRelease()
                                if (released) {
                                    // 提前松手 → 取消
                                    isLongPressing = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.stop_circle),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// ==================== 自定义形状 ====================

/**
 * 左半场形状（用于主队颜色覆盖）
 */
private class HalfLeftShape : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width / 2f, 0f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

/**
 * 右半场形状（用于客队颜色覆盖）
 */
private class HalfRightShape : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height)
            lineTo(size.width / 2f, size.height)
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

// ==================== 预览 ====================

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun MainScreenReadyPreviewSmall() {
    MaterialTheme {
        MainScreen(
            matchState = MATCH_STATE_READY,
            currentHalf = HALF_FIRST,
            mainTimeFormatted = "00:00",
            stoppageTimeFormatted = "00:00",
            homeTeamColor = Color(0xFF1565C0),
            awayTeamColor = Color(0xFFC62828),
            halfTimeSet = 45,
            onToggleTimer = {},
            onEndHalf = {},
            onOpenSettings = {},
            onSetMatchTime = {},
            onHomeColorClick = {},
            onAwayColorClick = {}
        )
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun MainScreenRunningPreviewLarge() {
    MaterialTheme {
        MainScreen(
            matchState = MATCH_STATE_RUNNING,
            currentHalf = HALF_FIRST,
            mainTimeFormatted = "23:45",
            stoppageTimeFormatted = "01:30",
            homeTeamColor = Color(0xFF1565C0),
            awayTeamColor = Color(0xFFC62828),
            halfTimeSet = 45,
            onToggleTimer = {},
            onEndHalf = {},
            onOpenSettings = {},
            onSetMatchTime = {},
            onHomeColorClick = {},
            onAwayColorClick = {}
        )
    }
}

@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun MainScreenPausedPreviewSquare() {
    MaterialTheme {
        MainScreen(
            matchState = MATCH_STATE_PAUSED,
            currentHalf = HALF_FIRST,
            mainTimeFormatted = "35:12",
            stoppageTimeFormatted = "02:00",
            homeTeamColor = Color(0xFF2E7D32),
            awayTeamColor = Color(0xFFB8960C),
            halfTimeSet = 45,
            onToggleTimer = {},
            onEndHalf = {},
            onOpenSettings = {},
            onSetMatchTime = {},
            onHomeColorClick = {},
            onAwayColorClick = {}
        )
    }
}
