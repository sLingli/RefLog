package com.reflog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.min

// 滚轮配置
internal val NUMBER_ITEM_HEIGHT = 40.dp
internal const val NUMBER_VISIBLE_COUNT = 5
internal val NUMBER_WHEEL_HEIGHT = NUMBER_ITEM_HEIGHT * NUMBER_VISIBLE_COUNT
internal const val NUMBER_INFINITE_COUNT = 100000
internal val NUMBER_RANGE = (0..9).toList()

/**
 * 单列数字滚轮
 */
@Composable
internal fun NumberWheel(
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
                .height(NUMBER_ITEM_HEIGHT)
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
            items(count = NUMBER_INFINITE_COUNT) { index ->
                val realIndex = index % NUMBER_RANGE.size
                val number = NUMBER_RANGE[realIndex]

                val scale = calculateNumberItemScale(listState, index)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NUMBER_ITEM_HEIGHT),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        fontSize = (24 * scale).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.3f + 0.7f * (scale - 0.6f) / 0.9f
                        )
                    )
                }
            }
        }
    }
}

/**
 * 计算单个 item 的缩放值（与 ColorWheel 逻辑一致）
 */
@Composable
internal fun calculateNumberItemScale(listState: LazyListState, index: Int): Float {
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
 * 记录滚轮中心位置的数字
 */
@Composable
internal fun rememberCenterNumber(listState: LazyListState): State<Int> {
    return remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
            val centerItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                abs(itemCenter - viewportCenter)
            }
            if (centerItem != null) {
                NUMBER_RANGE[centerItem.index % NUMBER_RANGE.size]
            } else {
                0
            }
        }
    }
}
