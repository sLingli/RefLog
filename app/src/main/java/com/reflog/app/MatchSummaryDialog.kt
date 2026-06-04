package com.reflog.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// 语义颜色（不受主题影响）
private val YellowCardColor = Color(0xFFFFEB3B)
private val RedCardColor = Color(0xFFF44336)
private val StoppageGreenColor = Color(0xFF00FF00)
private val InjuryBlueColor = Color(0xFF2196F3)

/**
 * 比赛总结弹窗
 */
@Composable
fun MatchSummaryDialog(
    isHistory: Boolean = false,
    halfTimeMinutes: Int,
    homeGoals: Int,
    awayGoals: Int,
    yellowCount: Int,
    redCount: Int,
    firstHalfStoppage: String,
    secondHalfStoppage: String,
    events: List<MatchEvent>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        MatchSummaryDialogContent(
            isHistory = isHistory,
            halfTimeMinutes = halfTimeMinutes,
            homeGoals = homeGoals,
            awayGoals = awayGoals,
            yellowCount = yellowCount,
            redCount = redCount,
            firstHalfStoppage = firstHalfStoppage,
            secondHalfStoppage = secondHalfStoppage,
            events = events,
            onCloseClick = onDismiss
        )
    }
}

/**
 * 弹窗内容
 */
@Composable
fun MatchSummaryDialogContent(
    isHistory: Boolean,
    halfTimeMinutes: Int,
    homeGoals: Int,
    awayGoals: Int,
    yellowCount: Int,
    redCount: Int,
    firstHalfStoppage: String,
    secondHalfStoppage: String,
    events: List<MatchEvent>,
    onCloseClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题 Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_trophy),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (isHistory) R.string.title_history_details
                    else R.string.title_summary
                ),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 统计卡片 Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 时长 Duration
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_history),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.summary_duration, halfTimeMinutes),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 比分 Score
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.sports_soccer),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.summary_score, homeGoals, awayGoals),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 黄牌和红牌行 Yellow & Red Cards Row
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 黄牌 Yellow Cards
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_card),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = YellowCardColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.summary_yellow, yellowCount),
                            color = YellowCardColor,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // 红牌 Red Cards
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_card),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RedCardColor
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.summary_red, redCount),
                            color = RedCardColor,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 分割线 Divider
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 补时 Stoppage
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.more_time),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = StoppageGreenColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.summary_stoppage, firstHalfStoppage, secondHalfStoppage),
                        color = StoppageGreenColor,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 事件明细标题 Event Details Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_event_note),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.label_event_details),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 事件列表 Event List
        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.msg_no_events),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                events.forEach { event ->
                    EventDetailItem(event = event)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 关闭按钮 Close Button
        Button(
            onClick = onCloseClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = stringResource(R.string.btn_ok),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 14.sp
            )
        }
    }
}

/**
 * 事件明细项
 */
@Composable
fun EventDetailItem(event: MatchEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // 图标 Icon
        val iconRes = when {
            event.event.contains("Goal", ignoreCase = true) ||
            event.event.contains("进球", ignoreCase = true) -> R.drawable.sports_soccer
            event.event.contains("Yellow", ignoreCase = true) ||
            event.event.contains("Red", ignoreCase = true) ||
            event.event.contains("黄牌", ignoreCase = true) ||
            event.event.contains("红牌", ignoreCase = true) -> R.drawable.ic_card
            event.event.contains("Sub", ignoreCase = true) ||
            event.event.contains("换人", ignoreCase = true) -> R.drawable.ic_substitute
            event.event.contains("Injury", ignoreCase = true) ||
            event.event.contains("受伤", ignoreCase = true) -> R.drawable.ic_medical
            else -> R.drawable.ic_history
        }

        val iconTint = when {
            event.event.contains("Goal", ignoreCase = true) ||
            event.event.contains("进球", ignoreCase = true) -> Color.White
            event.event.contains("Yellow", ignoreCase = true) ||
            event.event.contains("黄牌", ignoreCase = true) -> YellowCardColor
            event.event.contains("Red", ignoreCase = true) ||
            event.event.contains("红牌", ignoreCase = true) -> RedCardColor
            event.event.contains("Injury", ignoreCase = true) ||
            event.event.contains("受伤", ignoreCase = true) -> InjuryBlueColor
            else -> MaterialTheme.colorScheme.primary
        }

        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = iconTint
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 文字 Text
        val contentText = if (event.detail.isNotEmpty()) event.detail else event.event
        Text(
            text = "[${event.timeStr}] $contentText",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

/**
 * 预览 - 有事件
 */
@Preview(showBackground = true)
@Composable
fun MatchSummaryDialogPreviewWithEvents() {
    val sampleEvents = listOf(
        MatchEvent(
            timeStr = "15:30",
            event = "Goal",
            detail = "Home #10",
            half = "1st",
            minute = 15
        ),
        MatchEvent(
            timeStr = "23:45",
            event = "Yellow Card",
            detail = "Away #7",
            half = "1st",
            minute = 23
        ),
        MatchEvent(
            timeStr = "55:00",
            event = "Sub",
            detail = "Home #11 → #22",
            half = "2nd",
            minute = 55
        )
    )

    MatchSummaryDialogContent(
        isHistory = false,
        halfTimeMinutes = 45,
        homeGoals = 2,
        awayGoals = 1,
        yellowCount = 3,
        redCount = 0,
        firstHalfStoppage = "2:30",
        secondHalfStoppage = "4:15",
        events = sampleEvents,
        onCloseClick = {}
    )
}

/**
 * 预览 - 无事件
 */
@Preview(showBackground = true)
@Composable
fun MatchSummaryDialogPreviewEmpty() {
    MatchSummaryDialogContent(
        isHistory = false,
        halfTimeMinutes = 45,
        homeGoals = 0,
        awayGoals = 0,
        yellowCount = 0,
        redCount = 0,
        firstHalfStoppage = "0:00",
        secondHalfStoppage = "0:00",
        events = emptyList(),
        onCloseClick = {}
    )
}

/**
 * 预览 - 历史记录详情
 */
@Preview(showBackground = true)
@Composable
fun MatchSummaryDialogPreviewHistory() {
    val sampleEvents = listOf(
        MatchEvent(
            timeStr = "32:10",
            event = "红牌",
            detail = "客队 #3",
            half = "1st",
            minute = 32
        )
    )

    MatchSummaryDialogContent(
        isHistory = true,
        halfTimeMinutes = 45,
        homeGoals = 1,
        awayGoals = 0,
        yellowCount = 2,
        redCount = 1,
        firstHalfStoppage = "3:00",
        secondHalfStoppage = "5:00",
        events = sampleEvents,
        onCloseClick = {}
    )
}
