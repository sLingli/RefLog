package com.example.myapplication

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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

// 颜色定义
private val DialogBackgroundColor = Color(0xFF424242)
private val ClearButtonColor = Color(0xFFFF3B30)      // 清空按钮 - 红色
private val CloseButtonColor = Color(0xFF00E676)       // 关闭按钮 - 绿色
private val DeleteButtonColor = Color(0xFFD32F2F)      // 删除按钮 - 红色
private val DateTextColor = Color(0xFF4CAF50)          // 日期文字 - 绿色
private val DurationTextColor = Color(0xFF888888)      // 时长文字 - 灰色
private val StoppageTextColor = Color(0xFFFF9800)      // 补时文字 - 橙色
private val CardBackgroundColor = Color(0xFF303030)    // 卡片背景

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
            .background(DialogBackgroundColor)
            .padding(16.dp)
    ) {
        // 顶部图标 History Icon
        Icon(
            painter = painterResource(id = R.drawable.ic_history),
            contentDescription = null,
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterHorizontally),
            tint = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 记录列表或空状态 Records List or Empty State
        if (records.isEmpty()) {
            // 无记录提示 No Records Message
            Text(
                text = stringResource(R.string.dialog_no_records),
                color = Color(0xFF666666),
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        } else {
            // 记录列表 Records List
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                records.forEach { record ->
                    key(record.id) {
                        SwipeableRecordItem(
                            record = record,
                            onClick = { onRecordClick(record) },
                            onDelete = { onDeleteRecord(record) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
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
                    containerColor = CloseButtonColor
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
 */
@Composable
fun SwipeableRecordItem(
    record: MatchRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipeDistance = -200f
    val animatedOffsetX by animateFloatAsState(targetValue = offsetX, label = "offsetX")
    var isDeleted by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !isDeleted,
        exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 删除按钮（背景层） Delete Button (Background Layer)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp)
            ) {
                IconButton(
                    onClick = {
                        isDeleted = true
                        onDelete()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(DeleteButtonColor)
                        .alpha((-animatedOffsetX / maxSwipeDistance).coerceIn(0f, 1f))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_delete_24),
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            // 记录卡片（前景层） Record Card (Foreground Layer)
            RecordCard(
                record = record,
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                offsetX = if (offsetX < maxSwipeDistance / 2) {
                                    maxSwipeDistance
                                } else {
                                    0f
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                offsetX = (offsetX + dragAmount).coerceIn(maxSwipeDistance, 0f)
                            }
                        )
                    }
                    .clickable {
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
 */
@Composable
fun RecordCard(
    record: MatchRecord,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackgroundColor)
            .padding(12.dp)
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
                color = DateTextColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            // 时长 Duration
            Text(
                text = stringResource(R.string.fmt_duration_simple, record.halfTimeMinutes),
                color = DurationTextColor,
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
            color = StoppageTextColor,
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
            color = Color.White,
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
                .background(DialogBackgroundColor)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 确认消息 Confirm Message
            Text(
                text = stringResource(R.string.msg_confirm_clear_all),
                color = Color.White,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
                        containerColor = Color(0xFF616161)
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.outline_close_24),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color.White
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
 * 预览 - 有记录
 * Preview - With Records
 */
@Preview(showBackground = true)
@Composable
fun HistoryDialogPreviewWithRecords() {
    val sampleRecords = listOf(
        MatchRecord(
            id = 1,
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
            id = 2,
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
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DialogBackgroundColor)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Clear All History?",
            color = Color.White,
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {},
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF616161)
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.outline_close_24),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.White
                )
            }

            Button(
                onClick = {},
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




