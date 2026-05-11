package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/**
 * 号码选择结果
 * Number Selection Result
 */
sealed class NumberSelectionResult {
    data class Confirmed(val number: String) : NumberSelectionResult()
    object Cancelled : NumberSelectionResult()
}

// 滚轮配置
private val NUMBER_ITEM_HEIGHT = 40.dp
private const val NUMBER_VISIBLE_COUNT = 5
private val NUMBER_WHEEL_HEIGHT = NUMBER_ITEM_HEIGHT * NUMBER_VISIBLE_COUNT
private const val NUMBER_INFINITE_COUNT = 100000
private val NUMBER_RANGE = (0..9).toList()

// 语义颜色
private val CancelButtonColor = Color(0xFFFF3B30)
private val ConfirmButtonColor = Color(0xFF00E676)

/**
 * 号码选择弹窗
 * Number Selection Dialog
 *
 * @param eventType 事件类型名称（如 "Yellow Card"）
 * @param team 队伍名称（如 "Home"）
 * @param teamColor 队伍颜色（Int）
 * @param eventIconInfo 事件图标信息
 * @param onDismiss 关闭弹窗回调
 * @param onResult 结果回调
 */
@Composable
fun NumberSelectionDialog(
    eventType: String,
    team: String,
    teamColor: Int,
    eventIconInfo: EventIconInfo,
    onDismiss: () -> Unit,
    onResult: (NumberSelectionResult) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        NumberSelectionContent(
            eventType = eventType,
            team = team,
            teamColor = teamColor,
            eventIconInfo = eventIconInfo,
            onResult = { result ->
                onResult(result)
                onDismiss()
            }
        )
    }
}

/**
 * 号码选择弹窗内容
 */
@Composable
fun NumberSelectionContent(
    eventType: String,
    team: String,
    teamColor: Int,
    eventIconInfo: EventIconInfo,
    onResult: (NumberSelectionResult) -> Unit
) {
    val tensStartIndex = NUMBER_INFINITE_COUNT / 2
    val onesStartIndex = NUMBER_INFINITE_COUNT / 2 + 1

    val tensListState = rememberLazyListState(
        initialFirstVisibleItemIndex = tensStartIndex - (NUMBER_VISIBLE_COUNT / 2)
    )
    val onesListState = rememberLazyListState(
        initialFirstVisibleItemIndex = onesStartIndex - (NUMBER_VISIBLE_COUNT / 2)
    )

    val tensValue by rememberCenterNumber(tensListState)
    val onesValue by rememberCenterNumber(onesListState)
    val selectedNumber = tensValue * 10 + onesValue

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题行：事件图标 + 事件名称
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = eventIconInfo.iconResId),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = eventIconInfo.iconColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = eventType,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 队伍名
        Text(
            text = team,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(teamColor)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 标签行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Text(
                text = stringResource(R.string.label_tens),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.label_ones),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 双列数字滚轮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(NUMBER_WHEEL_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberWheel(
                listState = tensListState,
                modifier = Modifier.weight(1f)
            )
            NumberWheel(
                listState = onesListState,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 当前选择显示
        Text(
            text = "# ${String.format(Locale.getDefault(), "%02d", selectedNumber)}",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 取消按钮
            Button(
                onClick = { onResult(NumberSelectionResult.Cancelled) },
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
                onClick = { onResult(NumberSelectionResult.Confirmed(String.format(Locale.getDefault(), "%02d", selectedNumber))) },
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

/**
 * 单列数字滚轮
 */
@Composable
private fun NumberWheel(
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
private fun calculateNumberItemScale(listState: LazyListState, index: Int): Float {
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
private fun rememberCenterNumber(listState: LazyListState): State<Int> {
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

/**
 * 预览 - 号码选择弹窗
 */
@Preview(showBackground = true)
@Composable
fun NumberSelectionDialogPreview() {
    NumberSelectionContent(
        eventType = "Yellow Card",
        team = "Home",
        teamColor = 0xFF1565C0.toInt(),
        eventIconInfo = EventIconInfo(R.drawable.ic_card, Color.Yellow),
        onResult = {}
    )
}
