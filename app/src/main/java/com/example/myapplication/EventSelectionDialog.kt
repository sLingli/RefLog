package com.example.myapplication

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventSelectionDialog(
    onEventSelected: (EventType) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // 获取震动器
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    // 事件列表
    val events = EventType.entries

    // Pager状态
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { events.size }
    )

    // 记录上一页，用于检测页面切换
    var previousPage by remember { mutableIntStateOf(0) }

    // 监听页面切换，触发短震动
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != previousPage) {
            previousPage = pagerState.currentPage
            vibrateShort(vibrator)
        }
    }

    // 请求焦点以接收表冠输入
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // 累计旋转量，用于表冠切换页面
    var accumulatedRotation by remember { mutableFloatStateOf(0f) }
    val rotationThreshold = 30f  // 旋转阈值

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(focusRequester)
            .onRotaryScrollEvent { event ->
                accumulatedRotation += event.verticalScrollPixels

                when {
                    accumulatedRotation > rotationThreshold -> {
                        accumulatedRotation = 0f
                        if (pagerState.currentPage < events.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                    accumulatedRotation < -rotationThreshold -> {
                        accumulatedRotation = 0f
                        if (pagerState.currentPage > 0) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    }
                }
                true
            }
            .focusable()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val eventType = events[page]
            EventPage(
                eventType = eventType,
                onClick = {
                    vibrateStrong(vibrator)
                    onEventSelected(eventType)
                }
            )
        }

        // 底部页面指示器
        PageIndicator(
            pageCount = events.size,
            currentPage = pagerState.currentPage,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
}

/**
 * 单个事件页面
 */
@Composable
private fun EventPage(
    eventType: EventType,
    onClick: () -> Unit
) {
    val eventName = stringResource(eventType.displayNameResId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(eventType.backgroundColor)
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

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
fun EventSelectionDialogPreviewSmall() {
    MaterialTheme {
        EventSelectionDialog(
            onEventSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(device = Devices.WEAR_OS_LARGE_ROUND, showSystemUi = true)
@Composable
fun EventSelectionDialogPreviewLarge() {
    MaterialTheme {
        EventSelectionDialog(
            onEventSelected = {},
            onDismiss = {}
        )
    }
}
