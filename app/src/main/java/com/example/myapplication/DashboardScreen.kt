package com.example.myapplication

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.util.Calendar

/**
 * 仪表盘首页 - DashboardScreen
 *
 * 从上到下四大区块：
 * 1. 顶部状态区 (Header)：问候语 + 手表同步胶囊
 * 2. 核心数据看板 (Dashboard Card)：甜甜圈占位 + 2x2 统计网格
 * 3. 快捷行动区 (Quick Actions)：独立计时 / 赛事预设
 * 4. 近期执法 (Recent Matches)：真实历史记录卡片 / 空状态
 */
@Composable
fun DashboardScreen(
    state: DashboardState = DashboardState(),
    onQuickMatch: () -> Unit = {},
    onMatchTemplates: () -> Unit = {},
    onRecordClick: (MatchRecord) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ═══════════════════════════════════════
        // 1. 顶部状态区 (Header)
        // ═══════════════════════════════════════
        item {
            HeaderSection()
        }

        // ═══════════════════════════════════════
        // 2. 核心数据看板 (Dashboard Card)
        // ═══════════════════════════════════════
        item {
            DashboardCard(
                totalMatches = state.totalMatches,
                totalGoals = state.totalGoals,
                totalYellowCards = state.totalYellowCards,
                totalRedCards = state.totalRedCards,
            )
        }

        // ═══════════════════════════════════════
        // 3. 快捷行动区 (Quick Actions)
        // ═══════════════════════════════════════
        item {
            QuickActionsSection(
                onQuickMatch = onQuickMatch,
                onMatchTemplates = onMatchTemplates,
            )
        }

        // ═══════════════════════════════════════
        // 4. 近期执法 (Recent Matches)
        // ═══════════════════════════════════════
        item {
            RecentMatchesSection(
                recentRecords = state.recentRecords,
                onRecordClick = onRecordClick,
            )
        }
    }
}

// ═══════════════════════════════════════════════
// 1. 顶部状态区
// ═══════════════════════════════════════════════

@Composable
private fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧问候语
        Text(
            text = getGreetingText(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
        )

        // 右侧手表同步胶囊
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.watch),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.dashboard_watch_sync),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/**
 * 根据当前时间返回问候语
 */
@Composable
private fun getGreetingText(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour in 6..11 -> stringResource(R.string.greeting_morning)
        hour in 12..13 -> stringResource(R.string.greeting_noon)
        hour in 14..17 -> stringResource(R.string.greeting_afternoon)
        hour in 18..22 -> stringResource(R.string.greeting_evening)
        else -> stringResource(R.string.greeting_night)
    }
    return stringResource(R.string.dashboard_greeting, greeting)
}

// ═══════════════════════════════════════════════
// 2. 核心数据看板
// ═══════════════════════════════════════════════

@Composable
private fun DashboardCard(
    totalMatches: Int,
    totalGoals: Int,
    totalYellowCards: Int,
    totalRedCards: Int,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧 40%：甜甜圈图表
            Box(
                modifier = Modifier
                    .weight(0.26f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    yellowCardCount = totalYellowCards,
                    redCardCount = totalRedCards,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧 60%：2x2 统计网格
            Column(
                modifier = Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatItem(
                        modifier = Modifier.weight(1f),
                        value = totalMatches.toString(),
                        label = stringResource(R.string.dashboard_total_matches),
                    )
                    StatItem(
                        modifier = Modifier.weight(1f),
                        value = totalGoals.toString(),
                        label = stringResource(R.string.dashboard_goals),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    StatItem(
                        modifier = Modifier.weight(1f),
                        value = totalYellowCards.toString(),
                        label = stringResource(R.string.dashboard_yellow_cards),
                        valueColor = Color(0xFFFFEB3B),
                    )
                    StatItem(
                        modifier = Modifier.weight(1f),
                        value = totalRedCards.toString(),
                        label = stringResource(R.string.dashboard_red_cards),
                        valueColor = Color(0xFFF44336),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════
// DonutChart 甜甜圈图表
// ═══════════════════════════════════════════════

/**
 * 红黄牌比例环 — Donut Chart
 *
 * 极简风格：中心完全留空，仅展示圆弧比例。
 * - 总牌数为 0：绘制 360° 完整圆环（空状态占位色）
 * - 总牌数 > 0：按黄/红比例分两段首尾相连
 * - 首次出现时带 ~1000ms 的生长动画 (0° → 360°)
 *
 * @param yellowCardCount 黄牌数量
 * @param redCardCount 红牌数量
 */
@Composable
fun DonutChart(
    yellowCardCount: Int,
    redCardCount: Int,
) {
    val total = yellowCardCount + redCardCount
    val sweepAngle = remember { Animatable(0f) }

    // 颜色定义
    val yellowColor = Color(0xFFFFD700)
    val redColor = Color(0xFFFF5252)
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant

    // 线宽和圆角
    val strokeWidth = 18.dp

    // 生长动画
    LaunchedEffect(Unit) {
        sweepAngle.animateTo(
            targetValue = 360f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
    }

    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val canvasSize = minOf(size.width, size.height)
        val arcSize = Size(canvasSize, canvasSize)
        val topLeft = Offset(
            x = (size.width - canvasSize) / 2f,
            y = (size.height - canvasSize) / 2f
        )

        // 从顶部开始 (-90°)，即 12 点钟方向
        val startAngleOffset = -90f
        val currentSweep = sweepAngle.value

        if (total == 0) {
            // 空状态：完整圆环
            drawArc(
                color = emptyColor,
                startAngle = startAngleOffset,
                sweepAngle = currentSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(
                    width = strokeWidth.toPx(),
                    cap = StrokeCap.Round
                )
            )
        } else {
            val yellowFraction = yellowCardCount.toFloat() / total
            val redFraction = redCardCount.toFloat() / total
            val yellowSweep = yellowFraction * currentSweep
            val redSweep = redFraction * currentSweep

            // 黄牌弧段
            if (yellowSweep > 0f) {
                drawArc(
                    color = yellowColor,
                    startAngle = startAngleOffset,
                    sweepAngle = yellowSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            // 红牌弧段：紧接黄牌之后
            if (redSweep > 0f) {
                drawArc(
                    color = redColor,
                    startAngle = startAngleOffset + yellowSweep,
                    sweepAngle = redSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = strokeWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

/**
 * 统计项：大数字 + 小标签
 */
@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

// ═══════════════════════════════════════════════
// 3. 快捷行动区
// ═══════════════════════════════════════════════

@Composable
private fun QuickActionsSection(
    onQuickMatch: () -> Unit,
    onMatchTemplates: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左侧：选择赛事
        QuickActionCard(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.baseline_play_arrow_24,
            iconSize = 28.dp,
            label = stringResource(R.string.dashboard_match_templates),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onMatchTemplates,
        )

        // 右侧：快速开球
        QuickActionCard(
            modifier = Modifier.weight(1f),
            iconRes = R.drawable.lightning,
            iconSize = 24.dp,
            label = stringResource(R.string.dashboard_quick_match),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onQuickMatch,
        )
    }
}

/**
 * 快捷行动卡片按钮
 */
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    iconRes: Int,
    iconSize: Dp = 24.dp,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(iconSize),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
            )
        }
    }
}

// ═══════════════════════════════════════════════
// 4. 近期执法
// ═══════════════════════════════════════════════

/**
 * 近期执法区域
 *
 * 当有历史记录时，展示最近 3 条比赛卡片；
 * 当无记录时，显示空状态引导。
 */
@Composable
private fun RecentMatchesSection(
    recentRecords: List<MatchRecord>,
    onRecordClick: (MatchRecord) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.dashboard_recent_matches),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (recentRecords.isEmpty()) {
            EmptyRecentMatchesPlaceholder()
        } else {
            recentRecords.forEachIndexed { index, record ->
                MatchRecentCard(
                    record = record,
                    onClick = { onRecordClick(record) },
                )
                if (index < recentRecords.lastIndex) {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

/**
 * 空状态占位卡片
 *
 * 当近期执法区域无比赛记录时展示，引导用户开始第一场比赛。
 */
@Composable
private fun EmptyRecentMatchesPlaceholder() {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_history),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.dashboard_no_records),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.dashboard_no_records_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

/**
 * 近期执法真实卡片
 *
 * 展示一条 [MatchRecord] 的摘要信息：
 * 日期 + 时长 / 比分 / 事件统计（进球 · 黄牌 · 红牌）。
 * 复用与 HistoryDialog.RecordCard 一致的视觉风格。
 */
@Composable
private fun MatchRecentCard(
    record: MatchRecord,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 日期 + 时长
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.fmt_duration_simple, record.halfTimeMinutes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 比分 + 事件统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 比分
                Text(
                    text = "${record.homeGoals} : ${record.awayGoals}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(modifier = Modifier.weight(1f))

                // 事件统计矢量图标
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EventIconStat(
                        iconRes = R.drawable.sports_soccer,
                        count = record.goalCount,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    EventIconStat(
                        iconRes = R.drawable.ic_card,
                        count = record.yellowCount,
                        tint = Color(0xFFFFEB3B),
                    )
                    EventIconStat(
                        iconRes = R.drawable.ic_card,
                        count = record.redCount,
                        tint = Color(0xFFF44336),
                    )
                }
            }
        }
    }
}

/**
 * 事件统计图标 + 数字
 */
@Composable
private fun EventIconStat(
    iconRes: Int,
    count: Int,
    tint: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = tint,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ═══════════════════════════════════════════════
// Preview
// ═══════════════════════════════════════════════

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenPreview() {
    MaterialTheme {
        DashboardScreen(
            state = DashboardState(
                totalMatches = 12,
                totalGoals = 34,
                totalYellowCards = 8,
                totalRedCards = 2,
                recentRecords = listOf(
                    MatchRecord(
                        date = "2026-05-01",
                        halfTimeMinutes = 45,
                        firstHalfStoppage = "3 min",
                        secondHalfStoppage = "5 min",
                        totalStoppage = "8 min",
                        goalCount = 3,
                        yellowCount = 1,
                        redCount = 0,
                        substitutionCount = 0,
                        injuryCount = 0,
                        events = emptyList(),
                        homeGoals = 2,
                        awayGoals = 1,
                    ),
                    MatchRecord(
                        date = "2026-04-28",
                        halfTimeMinutes = 45,
                        firstHalfStoppage = "2 min",
                        secondHalfStoppage = "3 min",
                        totalStoppage = "5 min",
                        goalCount = 0,
                        yellowCount = 3,
                        redCount = 1,
                        substitutionCount = 0,
                        injuryCount = 0,
                        events = emptyList(),
                        homeGoals = 0,
                        awayGoals = 0,
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DashboardScreenEmptyPreview() {
    MaterialTheme {
        DashboardScreen(
            state = DashboardState(),
        )
    }
}
