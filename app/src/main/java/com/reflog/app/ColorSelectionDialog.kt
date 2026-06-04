package com.reflog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.abs
import kotlin.math.min

/**
 * 预定义颜色列表
 */
private val teamColors = listOf(
    0xFFF44336.toInt(), // 红 (Index 0)
    0xFF2196F3.toInt(), // 蓝 (Index 1)
    0xFF4CAF50.toInt(), // 绿
    0xFFFFEB3B.toInt(), // 黄
    0xFFFFFFFF.toInt(), // 白
    0xFF000000.toInt(), // 黑
    0xFF9C27B0.toInt(), // 紫
    0xFFFF9800.toInt()  // 橙
)

/** 滚轮可见数量（奇数，中心为选中项） */
private const val VISIBLE_ITEM_COUNT = 5
private val ITEM_HEIGHT = 48.dp
private val WHEEL_HEIGHT = ITEM_HEIGHT * VISIBLE_ITEM_COUNT
private const val INFINITE_COUNT = 100000

/**
 * 颜色选择弹窗
 * @param homeTeamColor 当前主队颜色
 * @param awayTeamColor 当前客队颜色
 * @param onDismiss 关闭弹窗回调
 * @param onConfirm 确认回调（主队颜色, 客队颜色）
 */
@Composable
fun ColorSelectionDialog(
    homeTeamColor: Int,
    awayTeamColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (homeColor: Int, awayColor: Int) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        ColorSelectionContent(
            initialHomeColor = homeTeamColor,
            initialAwayColor = awayTeamColor,
            onConfirm = { home, away ->
                onConfirm(home, away)
                onDismiss()
            }
        )
    }
}

/**
 * 颜色选择弹窗内容
 */
@Composable
fun ColorSelectionContent(
    initialHomeColor: Int,
    initialAwayColor: Int,
    onConfirm: (homeColor: Int, awayColor: Int) -> Unit
) {
    val initialHomeIndex = teamColors.indexOf(initialHomeColor).let { if (it >= 0) it else 1 }
    val initialAwayIndex = teamColors.indexOf(initialAwayColor).let { if (it >= 0) it else 0 }

    val centerOffset = INFINITE_COUNT / 2
    val homeStartIndex = centerOffset - (centerOffset % teamColors.size) + initialHomeIndex
    val awayStartIndex = centerOffset - (centerOffset % teamColors.size) + initialAwayIndex

    val homeListState = rememberLazyListState(
        initialFirstVisibleItemIndex = homeStartIndex - (VISIBLE_ITEM_COUNT / 2)
    )
    val awayListState = rememberLazyListState(
        initialFirstVisibleItemIndex = awayStartIndex - (VISIBLE_ITEM_COUNT / 2)
    )

    // 计算中心颜色
    val homeSelectedColor by rememberCenterColor(homeListState)
    val awaySelectedColor by rememberCenterColor(awayListState)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Text(
            text = stringResource(R.string.title_set_color),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 队伍标签行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = stringResource(R.string.team_home),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.team_away),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 双列颜色滚轮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WHEEL_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorWheel(
                listState = homeListState,
                modifier = Modifier.weight(1f)
            )
            ColorWheel(
                listState = awayListState,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 预览区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(homeSelectedColor))
            )
            Text(
                text = "VS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(awaySelectedColor))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 确认按钮
        Button(
            onClick = { onConfirm(homeSelectedColor, awaySelectedColor) },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00E676)
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

/**
 * 单列颜色滚轮
 */
@Composable
private fun ColorWheel(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    // 磁吸吸附：滚动停止时自动对齐到最近的中心项
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (!isScrolling) {
                    val layoutInfo = listState.layoutInfo
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centerItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        abs(item.offset + item.size / 2 - viewportCenter)
                    }
                    if (centerItem != null) {
                        val itemCenter = centerItem.offset + centerItem.size / 2
                        val scrollOffset = itemCenter - viewportCenter
                        if (abs(scrollOffset) > 1) {
                            listState.animateScrollBy(scrollOffset.toFloat())
                        }
                    }
                }
            }
    }

    Box(modifier = modifier) {
        // 中心选中指示器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ITEM_HEIGHT)
                .align(Alignment.Center)
                .background(
                    Color.White.copy(alpha = 0.15f),
                    RoundedCornerShape(8.dp)
                )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count = INFINITE_COUNT) { index ->
                val realIndex = index % teamColors.size
                val colorInt = teamColors[realIndex]

                // 计算与中心的距离来实现缩放效果
                val scale = calculateItemScale(listState, index)


                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ITEM_HEIGHT)
                        .padding(vertical = ((ITEM_HEIGHT - (32 * scale).dp) / 2)),
                    contentAlignment = Alignment.Center
                ) {
                    // 白色需要灰色边框，其他颜色用白色边框
                    val isWhite = (colorInt == 0xFFFFFFFF.toInt())
                    val borderColor = if (isWhite) Color(0xFF888888) else Color.White
                    Box(
                        modifier = Modifier
                            .size((32 * scale).dp)
                            .clip(CircleShape)
                            .background(borderColor)
                            .padding(1.5.dp)
                            .clip(CircleShape)
                            .background(Color(colorInt))
                    )
                }
            }
        }
    }
}

/**
 * 计算单个 item 的缩放值
 */
@Composable
private fun calculateItemScale(listState: LazyListState, index: Int): Float {
    val layoutInfo = listState.layoutInfo
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
    return if (itemInfo != null) {
        val itemCenter = itemInfo.offset + itemInfo.size / 2
        val distance = abs(itemCenter - viewportCenter).toFloat()
        val maxDistance = layoutInfo.viewportEndOffset.toFloat() / 2
        val normalizedDistance = if (maxDistance > 0) min(distance / maxDistance, 1f) else 0f
        1.5f - 0.9f * normalizedDistance
    } else {
        1.0f
    }
}

/**
 * 记录滚轮中心位置的颜色
 */
@Composable
private fun rememberCenterColor(listState: LazyListState): State<Int> {
    return remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centerItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }
            if (centerItem != null) {
                teamColors[centerItem.index % teamColors.size]
            } else {
                teamColors[0]
            }
        }
    }
}
