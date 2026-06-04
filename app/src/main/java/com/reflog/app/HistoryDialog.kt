package com.reflog.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * 历史记录弹窗
 * History Dialog
 *
 * 显示比赛历史记录列表，支持：
 * - 滑动删除单条记录
 * - 清空所有记录
 * - 点击查看详情
 */

// 语义颜色（不受主题影响）
private val ClearButtonColor = Color(0xFFFF3B30)      // 清空按钮 - 红色
private val DeleteButtonColor = Color(0xFFD32F2F)      // 删除按钮 - 红色

/**
 * 历史记录弹窗
 * @param records 比赛记录列表
 * @param onDismiss 关闭弹窗回调
 * @param onRecordClick 点击记录回调
 * @param onDeleteRecord 删除单条记录回调
 * @param onClearAll 清空所有记录回调
 */
@Composable
fun HistoryDialog(
    records: List<MatchRecord>,
    onDismiss: () -> Unit,
    onRecordClick: (MatchRecord) -> Unit,
    onDeleteRecord: (MatchRecord) -> Unit,
    onClearAll: () -> Unit
) {
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        HistoryDialogContent(
            records = records,
            onRecordClick = onRecordClick,
            onDeleteRecord = onDeleteRecord,
            onClearClick = {
                if (records.isNotEmpty()) {
                    showConfirmClearDialog = true
                }
            },
            onCloseClick = onDismiss
        )
    }

    // 确认清空弹窗 Confirm Clear Dialog
    if (showConfirmClearDialog) {
        ConfirmClearDialog(
            onConfirm = {
                onClearAll()
                showConfirmClearDialog = false
                onDismiss()
            },
            onDismiss = { showConfirmClearDialog = false }
        )
    }
}

/**
 * 弹窗内容
 * Dialog Content
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HistoryDialogContent(
    records: List<MatchRecord>,
    onRecordClick: (MatchRecord) -> Unit,
    onDeleteRecord: (MatchRecord) -> Unit,
    onClearClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        // 记录列表或空状态 Records List or Empty State
        if (records.isEmpty()) {
            // 无记录提示 No Records Message
            Text(
                text = stringResource(R.string.dialog_no_records),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = TextAlign.Center
            )
        } else {
            // 记录列表 Records List
            LazyColumn(
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = records,
                    key = { it.id }
                ) { record ->
                    SwipeableRecordItem(
                        record = record,
                        onClick = { onRecordClick(record) },
                        onDelete = { onDeleteRecord(record) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部按钮 Bottom Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 清空按钮 Clear Button
            Button(
                onClick = onClearClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClearButtonColor
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_delete_24),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // 关闭按钮 Close Button
            Button(
                onClick = onCloseClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_close_24),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * 可滑动删除的记录项
 * Swipeable Record Item
 *
 * 向左滑动露出红色删除按钮，点击按钮删除，
 * 退出动画：水平收缩 + 淡出
 */
@Composable
fun SwipeableRecordItem(
    record: MatchRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val horizontalPadding = 12.dp
    val circleSize = 48.dp
    val maxSwipeDistancePx = with(density) {
        -(circleSize + horizontalPadding * 2).toPx()
    }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(durationMillis = 200),
        label = "offsetX"
    )
    var isDeleted by remember { mutableStateOf(false) }

    // 等动画播放完毕再真正从列表移除数据
    LaunchedEffect(isDeleted) {
        if (isDeleted) {
            delay(300)
            onDelete()
        }
    }

    AnimatedVisibility(
        visible = !isDeleted,
        modifier = modifier,
        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
            animationSpec = tween(300),
            shrinkTowards = Alignment.Top
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 删除按钮背景层 — 红色圆形 + 垃圾桶图标
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = horizontalPadding)
            ) {
                IconButton(
                    onClick = {
                        isDeleted = true
                        // onDelete() 由 LaunchedEffect 延迟调用
                    },
                    modifier = Modifier
                        .size(circleSize)
                        .clip(CircleShape)
                        .background(DeleteButtonColor)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_delete_24),
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(2.dp)
                    )
                }
            }

            // 记录卡片前景层
            RecordCard(
                record = record,
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // 滑过一半则锁定展开，否则回弹
                                offsetX = if (offsetX < maxSwipeDistancePx / 2) {
                                    maxSwipeDistancePx
                                } else {
                                    0f
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                offsetX = (offsetX + dragAmount).coerceIn(maxSwipeDistancePx, 0f)
                            }
                        )
                    },
                onClick = {
                    if (offsetX == 0f) {
                        onClick()
                    } else {
                        offsetX = 0f
                    }
                }
            )
        }
    }
}

/**
 * 记录卡片
 * Record Card
 *
 * 使用 MD3 Card 实现，Card 内置波纹剪裁，
 * 涟漪特效自动限制在圆角内部，不会溢出。
 */
@Composable
fun RecordCard(
    record: MatchRecord,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
        // 日期和时长行 Date and Duration Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 日期 Date
            Text(
                text = record.date,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // 时长 Duration
            Text(
                text = stringResource(R.string.fmt_duration_simple, record.halfTimeMinutes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 补时信息 Stoppage Info
        Text(
            text = stringResource(
                R.string.summary_stoppage,
                record.firstHalfStoppage,
                record.secondHalfStoppage
            ),
            color = MaterialTheme.colorScheme.tertiary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 事件统计 Event Stats
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 进球 Goals
            StatItem(
                iconRes = R.drawable.sports_soccer,
                count = record.goalCount,
                tint = Color.White
            )
            // 黄牌 Yellow Cards
            StatItem(
                iconRes = R.drawable.ic_card,
                count = record.yellowCount,
                tint = Color.Yellow
            )
            // 红牌 Red Cards
            StatItem(
                iconRes = R.drawable.ic_card,
                count = record.redCount,
                tint = Color.Red
            )
            // 换人 Substitutions
            StatItem(
                iconRes = R.drawable.ic_substitute,
                count = record.substitutionCount,
                tint = Color.Green
            )
            // 伤停 Injuries
            StatItem(
                iconRes = R.drawable.ic_medical,
                count = record.injuryCount,
                tint = Color(0xFF2196F3)
            )
        }
    }
    }
}

/**
 * 统计项
 * Stat Item
 */
@Composable
fun StatItem(
    iconRes: Int,
    count: Int,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Text(
            text = count.toString(),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp
        )
    }
}

/**
 * 确认清空弹窗
 * Confirm Clear Dialog
 */
@Composable
fun ConfirmClearDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 确认消息 Confirm Message
            Text(
                text = stringResource(R.string.msg_confirm_clear_all),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮行 Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 取消按钮 No Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 确认按钮 Yes Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ClearButtonColor
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
}

/**
 * 全屏历史页面内容（用于 ViewPager2）
 * Full-screen history page content for ViewPager2
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun HistoryPageContent(
    records: List<MatchRecord>,
    onRecordClick: (MatchRecord) -> Unit,
    onDeleteRecord: (MatchRecord) -> Unit,
    onClearAll: () -> Unit
) {
    var showConfirmClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {


        Spacer(modifier = Modifier.height(16.dp))

        if (records.isEmpty()) {
            Text(
                text = stringResource(R.string.dialog_no_records),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = records,
                    key = { it.id }
                ) { record ->
                    SwipeableRecordItem(
                        record = record,
                        onClick = { onRecordClick(record) },
                        onDelete = { onDeleteRecord(record) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 清空按钮
        if (records.isNotEmpty()) {
            Button(
                onClick = { showConfirmClearDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClearButtonColor
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_delete_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_clear_all_history),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }

    // 确认清空弹窗
    if (showConfirmClearDialog) {
        ConfirmClearDialog(
            onConfirm = {
                onClearAll()
                showConfirmClearDialog = false
            },
            onDismiss = { showConfirmClearDialog = false }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryDialogPreviewWithRecords() {
    val sampleRecords = listOf(
        MatchRecord(
            id = 1L,
            date = "2024-01-01",
            halfTimeMinutes = 45,
            firstHalfStoppage = "3:00",
            secondHalfStoppage = "5:00",
            totalStoppage = "8:00",
            goalCount = 3,
            yellowCount = 2,
            redCount = 1,
            substitutionCount = 4,
            injuryCount = 1,
            events = emptyList()
        ),
        MatchRecord(
            id = 2L,
            date = "2024-01-02",
            halfTimeMinutes = 45,
            firstHalfStoppage = "2:00",
            secondHalfStoppage = "4:00",
            totalStoppage = "6:00",
            goalCount = 2,
            yellowCount = 3,
            redCount = 0,
            substitutionCount = 6,
            injuryCount = 0,
            events = emptyList()
        )
    )

    HistoryDialogContent(
        records = sampleRecords,
        onRecordClick = {},
        onDeleteRecord = {},
        onClearClick = {},
        onCloseClick = {}
    )
}

/**
 * 预览 - 无记录
 * Preview - Empty
 */
@Preview(showBackground = true)
@Composable
fun HistoryDialogPreviewEmpty() {
    HistoryDialogContent(
        records = emptyList(),
        onRecordClick = {},
        onDeleteRecord = {},
        onClearClick = {},
        onCloseClick = {}
    )
}

/**
 * 预览 - 确认清空弹窗
 * Preview - Confirm Clear Dialog
 */
@Preview(showBackground = true)
@Composable
fun ConfirmClearDialogPreview() {
    ConfirmClearDialog(
        onConfirm = {},
        onDismiss = {}
    )
}
