package com.example.myapplication

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices

/**
 * 事件类型枚举
 */
enum class EventType(
    val displayNameResId: Int,
    val iconResId: Int,
    val backgroundColor: Color
) {
    YELLOW_CARD(
        displayNameResId = R.string.event_yellow,
        iconResId = R.drawable.ic_card,
        backgroundColor = Color(0xFFB8960C)  // 黄牌莫兰迪色
    ),
    RED_CARD(
        displayNameResId = R.string.event_red,
        iconResId = R.drawable.ic_card,
        backgroundColor = Color(0xFF8B1A1A)  // 红牌莫兰迪色
    ),
    INJURY(
        displayNameResId = R.string.event_injury,
        iconResId = R.drawable.ic_medical,
        backgroundColor = Color(0xFF2E7D9B)  // 伤病莫兰迪色
    ),
    GOAL(
        displayNameResId = R.string.event_goal,
        iconResId = R.drawable.sports_soccer,
        backgroundColor = Color(0xFF2E7D32)  // 进球莫兰迪色
    ),
    SUBSTITUTION(
        displayNameResId = R.string.event_substitute,
        iconResId = R.drawable.ic_substitute,
        backgroundColor = Color(0xFF7B1FA2)  // 换人莫兰迪色
    )
}

/**
 * 全屏翻页式事件选择弹窗
 */
@Composable
fun EventSelectionDialog(
    onEventSelected: (EventType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    // 震动器状态 - 懒加载
    var vibrator by remember { mutableStateOf<Vibrator?>(null) }

    // 事件列表
    val events = remember { EventType.entries }

    // 当前页面索引
    var currentPage by remember { mutableIntStateOf(0) }
    var previousPage by remember { mutableIntStateOf(0) }

    // 页面偏移量动画状态 (像素)
    val offsetX = remember { Animatable(0f) }

    // 屏幕宽度
    var screenWidth by remember { mutableFloatStateOf(0f) }

    // 拖拽状态
    var isDragging by remember { mutableStateOf(false) }

    // 初始化震动器
    LaunchedEffect(Unit) {
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(VibratorManager::class.java)
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
        focusRequester.requestFocus()
    }

    // 监听页面切换，触发短震动
    LaunchedEffect(currentPage) {
        if (currentPage != previousPage) {
            previousPage = currentPage
            vibrateShort(vibrator)
        }
    }

    // 拖拽手势状态 - 支持循环滑动
    val draggableState = rememberDraggableState { delta ->
        if (screenWidth > 0f) {
            coroutineScope.launch {
                // 允许无限滑动，不限制边界
                offsetX.snapTo(offsetX.value + delta)
            }
        }
    }

    // 旋转滚动处理
    var accumulatedRotation by remember { mutableFloatStateOf(0f) }
    val rotationThreshold = 30f

    // 检测屏幕形状
    val isRoundScreen = LocalConfiguration.current.isScreenRound
    val screenShape = if (isRoundScreen) CircleShape else RoundedCornerShape(16.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(screenShape)
            .focusRequester(focusRequester)
            .onRotaryScrollEvent { event ->
                if (!isDragging) {
                    accumulatedRotation += event.verticalScrollPixels
                    when {
                        accumulatedRotation > rotationThreshold -> {
                            accumulatedRotation = 0f
                            // 支持循环：从第0页到最后一页
                            currentPage = if (currentPage > 0) currentPage - 1 else events.size - 1
                            coroutineScope.launch {
                                offsetX.animateTo(-currentPage * screenWidth)
                            }
                        }
                        accumulatedRotation < -rotationThreshold -> {
                            accumulatedRotation = 0f
                            // 支持循环：从最后一页到第0页
                            currentPage = if (currentPage < events.size - 1) currentPage + 1 else 0
                            coroutineScope.launch {
                                offsetX.animateTo(-currentPage * screenWidth)
                            }
                        }
                    }
                }
                true
            }
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStarted = {
                    isDragging = true
                },
                onDragStopped = { velocity ->
                    isDragging = false

                    if (screenWidth > 0f) {
                        coroutineScope.launch {
                            // 计算当前偏移对应的"虚拟页面"
                            val virtualPage = -offsetX.value / screenWidth

                            // 计算最近的页面（可能是负数或超过最大值）
                            val nearestVirtualPage = if (abs(velocity) > 1000f) {
                                // 惯性滑动：根据速度方向决定
                                val velocityDirection = if (velocity > 0) -1 else 1
                                virtualPage.roundToInt() + velocityDirection
                            } else {
                                // 普通吸附：找最近页面
                                virtualPage.roundToInt()
                            }

                            // 将虚拟页面映射到实际页面（支持循环）
                            val actualPage = ((nearestVirtualPage % events.size) + events.size) % events.size
                            currentPage = actualPage

                            // 计算目标偏移（使用最短路径）
                            val targetOffset = -nearestVirtualPage * screenWidth

                            // 动画到目标位置
                            if (abs(velocity) > 1000f) {
                                // 使用衰减动画模拟惯性
                                offsetX.animateDecay(
                                    initialVelocity = velocity,
                                    animationSpec = exponentialDecay()
                                )
                            }
                            // 最终确保位置正确
                            offsetX.animateTo(targetOffset)
                        }
                    }
                }
            )
            .focusable()
    ) {
        // 记录屏幕宽度
        LaunchedEffect(constraints.maxWidth) {
            screenWidth = constraints.maxWidth.toFloat()
        }

        if (screenWidth > 0f) {
            // 循环渲染页面 - 渲染多个实例以支持无缝循环
            // 计算当前偏移对应的虚拟页面
            val virtualPage = -offsetX.value / screenWidth

            // 渲染当前可见区域周围的页面（前后各2页保证流畅）
            for (virtualIndex in (virtualPage.toInt() - 2)..(virtualPage.toInt() + 2)) {
                val actualIndex = ((virtualIndex % events.size) + events.size) % events.size
                val eventType = events[actualIndex]
                val pageOffsetX = virtualIndex * screenWidth + offsetX.value

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = pageOffsetX
                        }
                        .background(eventType.backgroundColor)
                ) {
                    EventPageContent(
                        eventType = eventType,
                        onClick = {
                            if (!isDragging && actualIndex == currentPage) {
                                vibrateStrong(vibrator)
                                onEventSelected(eventType)
                            }
                        }
                    )
                }
            }

            // 底部页面指示器
            PageIndicator(
                pageCount = events.size,
                currentPage = currentPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

/**
 * 单个事件页面内容
 */
@Composable
private fun EventPageContent(
    eventType: EventType,
    onClick: () -> Unit
) {
    val eventName = stringResource(eventType.displayNameResId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = eventType.iconResId),
                contentDescription = eventName,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = eventName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * 页面指示器（小圆点）
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (isSelected) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

/**
 * 短震动（页面切换时）
 */
private fun vibrateShort(vibrator: Vibrator?) {
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(30)
        }
    }
}

/**
 * 强震动（确认选择时）
 */
private fun vibrateStrong(vibrator: Vibrator?) {
    vibrator?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            it.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
        } else {
            @Suppress("DEPRECATION")
            it.vibrate(100)
        }
    }
}

// ==================== 旧版兼容接口 ====================
// 保留旧版签名以兼容现有调用

@Composable
fun EventSelectionDialog(
    onYellowCardClick: () -> Unit,
    onRedCardClick: () -> Unit,
    onInjuryClick: () -> Unit,
    onGoalClick: () -> Unit,
    onSubstitutionClick: () -> Unit,
    onCancelClick: () -> Unit,
) {
    EventSelectionDialog(
        onEventSelected = { eventType ->
            when (eventType) {
                EventType.YELLOW_CARD -> onYellowCardClick()
                EventType.RED_CARD -> onRedCardClick()
                EventType.INJURY -> onInjuryClick()
                EventType.GOAL -> onGoalClick()
                EventType.SUBSTITUTION -> onSubstitutionClick()
            }
        },
        onDismiss = onCancelClick
    )
}

// ==================== 预览 ====================

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun EventSelectionDialogPreviewSmall() {
    MaterialTheme {
        // 直接预览页面内容，避免触发系统服务调用
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EventType.YELLOW_CARD.backgroundColor)
        ) {
            EventPageContent(
                eventType = EventType.YELLOW_CARD,
                onClick = {}
            )
            PageIndicator(
                pageCount = 5,
                currentPage = 0,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Preview(device = WearDevices.LARGE_ROUND, showSystemUi = true)
@Composable
fun EventSelectionDialogPreviewLarge() {
    MaterialTheme {
        // 直接预览页面内容，避免触发系统服务调用
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(EventType.GOAL.backgroundColor)
        ) {
            EventPageContent(
                eventType = EventType.GOAL,
                onClick = {}
            )
            PageIndicator(
                pageCount = 5,
                currentPage = 3,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Preview(device = WearDevices.SQUARE, showSystemUi = true)
@Composable
fun EventSelectionDialogPreviewSquare() {
    MaterialTheme {
        // 直接预览页面内容，避免触发系统服务调用
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(EventType.RED_CARD.backgroundColor)
        ) {
            EventPageContent(
                eventType = EventType.RED_CARD,
                onClick = {}
            )
            PageIndicator(
                pageCount = 5,
                currentPage = 1,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
    }
}

