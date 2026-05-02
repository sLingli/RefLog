package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

// 颜色定义
private val YellowCardColor = Color(0xFFFFEB3B)   // 黄牌
private val RedCardColor = Color(0xFFF44336)      // 红牌
private val InjuryColor = Color(0xFF2196F3)       // 伤停
private val GoalColor = Color(0xFF000000)         // 进球
private val SubstitutionColor = Color(0xFF9C27B0) // 换人
private val CancelColor = Color(0xFF616161)       // 取消

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
            .background(Color(0xFF424242))
            .padding(12.dp)
    ) {
        // 第一行：黄牌 + 红牌
        // First Row: Yellow Card + Red Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 黄牌按钮 (Yellow Card Button)
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = YellowCardColor,
                onClick = { onEventSelected(EventType.YELLOW_CARD) }
            )

            // 红牌按钮 (Red Card Button)
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = RedCardColor,
                onClick = { onEventSelected(EventType.RED_CARD) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第二行：伤停 + 进球
        // Second Row: Injury + Goal
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 伤停按钮 (Injury Button)
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = InjuryColor,
                iconResId = R.drawable.ic_medical,
                onClick = { onEventSelected(EventType.INJURY) }
            )

            // 进球按钮 (Goal Button)
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = GoalColor,
                iconResId = R.drawable.sports_soccer,
                onClick = { onEventSelected(EventType.GOAL) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 第三行：换人 + 取消
        // Third Row: Substitution + Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 换人按钮 (Substitution Button)
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = SubstitutionColor,
                iconResId = R.drawable.ic_substitute,
                onClick = { onEventSelected(EventType.SUBSTITUTION) }
            )

            // 取消按钮 (Cancel Button)
            EventButton(
                modifier = Modifier.weight(1f),
                backgroundColor = CancelColor,
                iconResId = R.drawable.outline_close_24,
                onClick = { onEventSelected(EventType.CANCEL) }
            )
        }
    }
}

/**
 * 事件按钮组件
 * Event Button Component
 *
 * @param modifier Modifier
 * @param backgroundColor 背景颜色
 * @param iconResId 图标资源ID（可选）
 * @param onClick 点击回调
 */
@Composable
fun EventButton(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    iconResId: Int? = null,
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
        contentPadding = PaddingValues(0.dp)
    ) {
        if (iconResId != null) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * 预览
 * Preview
 */
@Preview(showBackground = true)
@Composable
fun EventSelectionDialogPreview() {
    EventSelectionContent(
        onEventSelected = {}
    )
}


