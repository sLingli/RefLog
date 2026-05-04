package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/**
 * 仪表盘首页 - DashboardScreen
 *
 * 从上到下四大区块：
 * 1. 顶部状态区 (Header)：问候语 + 手表同步胶囊
 * 2. 核心数据看板 (Dashboard Card)：甜甜圈占位 + 2x2 统计网格
 * 3. 快捷行动区 (Quick Actions)：独立计时 / 赛事预设
 * 4. 近期执法 (Recent Matches)：历史记录占位卡片
 */
@Composable
fun DashboardScreen(
    totalMatches: Int = 0,
    totalGoals: Int = 0,
    totalYellowCards: Int = 0,
    totalRedCards: Int = 0,
    onStartTimer: () -> Unit = {},
    onEventPreset: () -> Unit = {},
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
                totalMatches = totalMatches,
                totalGoals = totalGoals,
                totalYellowCards = totalYellowCards,
                totalRedCards = totalRedCards,
            )
        }

        // ═══════════════════════════════════════
        // 3. 快捷行动区 (Quick Actions)
        // ═══════════════════════════════════════
        item {
            QuickActionsSection(
                onStartTimer = onStartTimer,
                onEventPreset = onEventPreset,
            )
        }

        // ═══════════════════════════════════════
        // 4. 近期执法 (Recent Matches)
        // ═══════════════════════════════════════
        item {
            RecentMatchesSection()
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
                Text(
                    text = "⌚",
                    fontSize = 12.sp,
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
            // 左侧 40%：甜甜圈图表占位
            Box(
                modifier = Modifier
                    .weight(0.4f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                DonutPlaceholder()
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧 60%：2x2 统计网格
            Column(
                modifier = Modifier.weight(0.6f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        value = totalMatches.toString(),
                        label = stringResource(R.string.dashboard_total_matches),
                    )
                    StatItem(
                        value = totalGoals.toString(),
                        label = stringResource(R.string.dashboard_goals),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem(
                        value = totalYellowCards.toString(),
                        label = stringResource(R.string.dashboard_yellow_cards),
                        valueColor = Color(0xFFFFEB3B),
                    )
                    StatItem(
                        value = totalRedCards.toString(),
                        label = stringResource(R.string.dashboard_red_cards),
                        valueColor = Color(0xFFF44336),
                    )
                }
            }
        }
    }
}

/**
 * 甜甜圈图表占位符
 */
@Composable
private fun DonutPlaceholder() {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        // 外环占位
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            // 中心空洞
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "📊",
                    fontSize = 20.sp,
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
    value: String,
    label: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(
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
    onStartTimer: () -> Unit,
    onEventPreset: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 左侧：独立计时
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = "▶️",
            label = stringResource(R.string.dashboard_quick_timer),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onStartTimer,
        )

        // 右侧：赛事预设
        QuickActionCard(
            modifier = Modifier.weight(1f),
            icon = "🛠️",
            label = stringResource(R.string.dashboard_event_preset),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onEventPreset,
        )
    }
}

/**
 * 快捷行动卡片按钮
 */
@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: String,
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
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 22.sp,
            )
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

@Composable
private fun RecentMatchesSection() {
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

        // 占位卡片 1
        RecentMatchPlaceholderCard(
            matchTitle = "Team A  vs  Team B",
            score = "2 : 1",
            date = "2026-05-01",
            events = "⚽2  🟨1  🟥0",
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 占位卡片 2
        RecentMatchPlaceholderCard(
            matchTitle = "Team C  vs  Team D",
            score = "0 : 0",
            date = "2026-04-28",
            events = "⚽0  🟨3  🟥1",
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 占位卡片 3
        RecentMatchPlaceholderCard(
            matchTitle = "Team E  vs  Team F",
            score = "3 : 2",
            date = "2026-04-25",
            events = "⚽5  🟨2  🟥0",
        )
    }
}

/**
 * 近期执法占位卡片
 */
@Composable
private fun RecentMatchPlaceholderCard(
    matchTitle: String,
    score: String,
    date: String,
    events: String,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧赛事信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = matchTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 中间比分
            Text(
                text = score,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            // 右侧事件统计
            Text(
                text = events,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
            totalMatches = 12,
            totalGoals = 34,
            totalYellowCards = 8,
            totalRedCards = 2,
        )
    }
}
