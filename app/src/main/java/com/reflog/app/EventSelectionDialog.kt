package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

/**
 * 事件选择弹窗
 * Event Selection Dialog
 *
 * 包含以下选项:
 * - 黄牌 (Yellow Card)
 * - 红牌 (Red Card)
 * - 伤停 (Injury)
 * - 进球 (Goal)
 * - 换人 (Substitution)
 * - 取消 (Cancel)
 */

// 语义颜色（不受主题影响）
private val YellowCardColor = Color(0xFFFFEB3B)   // 黄牌
private val RedCardColor = Color(0xFFF44336)      // 红牌
private val InjuryColor = Color(0xFF2196F3)       // 伤停
private val GoalColor = Color(0xFF2E7D32)         // 进球
private val SubstitutionColor = Color(0xFF9C27B0) // 换人

/**
 * 事件类型枚举
 * Event Type Enum
 */
enum class EventType {
    YELLOW_CARD,    // 黄牌
    RED_CARD,       // 红牌
    INJURY,         // 伤停
    GOAL,           // 进球
    SUBSTITUTION,   // 换人
    CANCEL          // 取消
}

/**
 * 事件选择弹窗
 * @param onDismiss 关闭弹窗回调
 * @param onEventSelected 事件选择回调
 */
@Composable
fun EventSelectionDialog(
    onDismiss: () -> Unit,
    onEventSelected: (EventType) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        EventSelectionContent(
            onEventSelected = { eventType ->
                onEventSelected(eventType)
                if (eventType == EventType.CANCEL) {
                    onDismiss()
                }
            }
        )
    }
}

/**
 * 弹窗内容
 * Dialog Content
 */
@Composable
fun EventSelectionContent(
    onEventSelected: (EventType) -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp)
    ) {
        // 第一行：黄牌 + 红牌
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = YellowCardColor,
                iconResId = R.drawable.ic_card,
                label = stringResource(R.string.event_yellow),
                iconTint = Color(0xFF1A1A1A),
                onClick = { onEventSelected(EventType.YELLOW_CARD) }
            )

            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = RedCardColor,
                iconResId = R.drawable.ic_card,
                label = stringResource(R.string.event_red),
                iconTint = Color.White,
                onClick = { onEventSelected(EventType.RED_CARD) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第二行：伤停 + 进球
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = InjuryColor,
                iconResId = R.drawable.ic_medical,
                label = stringResource(R.string.event_injury),
                onClick = { onEventSelected(EventType.INJURY) }
            )

            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = GoalColor,
                iconResId = R.drawable.sports_soccer,
                label = stringResource(R.string.event_goal),
                onClick = { onEventSelected(EventType.GOAL) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第三行：换人 + 取消
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = SubstitutionColor,
                iconResId = R.drawable.ic_substitute,
                label = stringResource(R.string.event_substitute),
                onClick = { onEventSelected(EventType.SUBSTITUTION) }
            )

            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                iconResId = R.drawable.outline_close_24,
                iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onEventSelected(EventType.CANCEL) }
            )
        }
    }
}

/**
 * 事件按钮组件
 * Event Button Component
 */
@Composable
fun EventButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    iconResId: Int? = null,
    label: String? = null,
    iconTint: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(72.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        ),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconResId != null) {
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = iconTint
                )
            }
            if (label != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 预览 - 事件选择弹窗
 * Preview - Event Selection Dialog
 */
@Preview(showBackground = true)
@Composable
fun EventSelectionDialogPreview() {
    EventSelectionContent(
        onEventSelected = {}
    )
}
