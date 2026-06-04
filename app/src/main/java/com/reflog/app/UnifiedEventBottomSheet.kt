package com.reflog.app

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * 统一事件记录底部面板
 *
 * 三合一操作台：事件选择 (LazyRow) → 队伍选择 (Row) → 号码选择 (双滚轮) → 确认按钮
 *
 * @param sheetState     由调用方控制的 ModalBottomSheet 状态
 * @param homeColor      主队颜色（动态传入）
 * @param awayColor      客队颜色（动态传入）
 * @param onDismiss      面板关闭回调
 * @param onConfirm      确认回调：(事件类型, 队伍选择, 号码字符串)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedEventBottomSheet(
    sheetState: SheetState,
    homeColor: Color,
    awayColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (eventType: EventType, team: TeamSelection, number: String) -> Unit,
) {
    val context = LocalContext.current

    // 事件类型选择
    var selectedEventType by remember { mutableStateOf<EventType?>(null) }
    // 队伍选择
    var selectedTeam by remember { mutableStateOf<TeamSelection?>(null) }

    // 号码滚轮
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

    // 判断是否需要队伍/号码选择（受伤、换人可直接记录）
    val requiresTeamAndNumber = selectedEventType?.requiresTeamAndNumber() != false

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ═══════════════════════════════════════
            // 第一层：事件选择 (横向滑动 LazyRow)
            // ═══════════════════════════════════════
            Text(
                text = stringResource(R.string.label_event_details),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            val eventItems = remember {
                listOf(
                    EventItem(EventType.GOAL, R.drawable.sports_soccer, R.string.event_goal, Color(0xFF2E7D32)),
                    EventItem(EventType.YELLOW_CARD, R.drawable.ic_card, R.string.event_yellow, Color(0xFFFFEB3B)),
                    EventItem(EventType.RED_CARD, R.drawable.ic_card, R.string.event_red, Color(0xFFF44336)),
                    EventItem(EventType.INJURY, R.drawable.ic_medical, R.string.event_injury, Color(0xFF2196F3)),
                    EventItem(EventType.SUBSTITUTION, R.drawable.ic_substitute, R.string.event_substitute, Color(0xFF9C27B0)),
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(eventItems) { item ->
                    val isSelected = selectedEventType == item.eventType
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedEventType = item.eventType },
                        label = {
                            Text(
                                text = stringResource(item.labelRes),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = if (isSelected) item.color else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = item.color.copy(alpha = 0.3f),
                            selectedLabelColor = item.color,
                            selectedLeadingIconColor = item.color,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }

            // ═══════════════════════════════════════
            // 第二层：队伍选择 (仅在需要时显示)
            // ═══════════════════════════════════════
            if (requiresTeamAndNumber) {
                Text(
                    text = stringResource(R.string.title_select_team_generic),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TeamToggleButton(
                        modifier = Modifier.weight(1f),
                        teamName = stringResource(R.string.team_home),
                        teamColor = homeColor,
                        isSelected = selectedTeam == TeamSelection.HOME,
                        onClick = { selectedTeam = TeamSelection.HOME },
                    )
                    TeamToggleButton(
                        modifier = Modifier.weight(1f),
                        teamName = stringResource(R.string.team_away),
                        teamColor = awayColor,
                        isSelected = selectedTeam == TeamSelection.AWAY,
                        onClick = { selectedTeam = TeamSelection.AWAY },
                    )
                }
            }

            // ═══════════════════════════════════════
            // 第三层：号码选择 (双滚轮，仅在需要时显示)
            // ═══════════════════════════════════════
            if (requiresTeamAndNumber) {
                // 双列数字滚轮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(NUMBER_WHEEL_HEIGHT),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    NumberWheel(
                        listState = tensListState,
                        modifier = Modifier.weight(1f),
                    )
                    NumberWheel(
                        listState = onesListState,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // ═══════════════════════════════════════
            // 底部：确认按钮
            // ═══════════════════════════════════════
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    when {
                        selectedEventType == null -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.event_select_first),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        requiresTeamAndNumber && selectedTeam == null -> {
                            Toast.makeText(
                                context,
                                context.getString(R.string.event_select_team_first),
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                        else -> {
                            onConfirm(
                                selectedEventType!!,
                                selectedTeam ?: TeamSelection.HOME,
                                String.format(Locale.getDefault(), "%02d", selectedNumber),
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.event_confirm_record),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════
// 辅助数据类与组件
// ═══════════════════════════════════════════════

/**
 * 事件项描述
 */
private data class EventItem(
    val eventType: EventType,
    val iconRes: Int,
    val labelRes: Int,
    val color: Color,
)

/**
 * 队伍切换按钮
 */
@Composable
private fun TeamToggleButton(
    modifier: Modifier = Modifier,
    teamName: String,
    teamColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val isLightColor = teamColor == Color.White || teamColor == Color(0xFFFFFF)
    val contentColor = if (isSelected) {
        if (isLightColor) Color.Black else Color.White
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val containerColor = if (isSelected) teamColor else MaterialTheme.colorScheme.surfaceContainerHighest

    Surface(
        modifier = modifier.height(48.dp),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        tonalElevation = if (isSelected) 4.dp else 0.dp,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = teamName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
            )
        }
    }
}

// ═══════════════════════════════════════════════
// Previews
// ═══════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun UnifiedEventBottomSheetPreview() {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    MaterialTheme {
        UnifiedEventBottomSheet(
            sheetState = sheetState,
            homeColor = Color(0xFF1565C0),
            awayColor = Color(0xFFC62828),
            onDismiss = {},
            onConfirm = { _, _, _ -> },
        )
    }
}
