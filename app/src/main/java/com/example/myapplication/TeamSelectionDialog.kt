package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * 队伍选择弹窗
 * Team Selection Dialog
 *
 * 选择主队或客队，用于记录事件（黄牌、红牌、进球）
 * Select home or away team for recording events (yellow card, red card, goal)
 */

// 颜色定义
private val DialogBackgroundColor = Color(0xFF424242)
private val CancelButtonColor = Color(0xFF333333)
private val CancelIconColor = Color(0xFFAAAAAA)

/**
 * 队伍选择结果
 * Team Selection Result
 */
enum class TeamSelection {
    HOME,       // 主队
    AWAY,       // 客队
    CANCEL      // 取消
}

/**
 * 事件图标信息
 * Event Icon Info
 */
data class EventIconInfo(
    val iconResId: Int,
    val iconColor: Color
)

/**
 * 获取事件图标信息
 * Get Event Icon Info
 */
fun getEventIconInfo(eventType: EventType): EventIconInfo {
    return when (eventType) {
        EventType.YELLOW_CARD -> EventIconInfo(R.drawable.ic_card, Color.Yellow)
        EventType.RED_CARD -> EventIconInfo(R.drawable.ic_card, Color.Red)
        EventType.GOAL -> EventIconInfo(R.drawable.sports_soccer, Color.White)
        else -> EventIconInfo(R.drawable.ic_card, Color.White)
    }
}

/**
 * 队伍选择弹窗
 * @param eventType 事件类型
 * @param eventTitle 事件标题（本地化后的字符串）
 * @param homeTeamColor 主队颜色
 * @param awayTeamColor 客队颜色
 * @param onDismiss 关闭弹窗回调
 * @param onTeamSelected 队伍选择回调
 */
@Composable
fun TeamSelectionDialog(
    eventType: EventType,
    eventTitle: String,
    homeTeamColor: Int,
    awayTeamColor: Int,
    onDismiss: () -> Unit,
    onTeamSelected: (TeamSelection) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        TeamSelectionContent(
            eventType = eventType,
            eventTitle = eventTitle,
            homeTeamColor = homeTeamColor,
            awayTeamColor = awayTeamColor,
            onTeamSelected = { selection ->
                onTeamSelected(selection)
                if (selection == TeamSelection.CANCEL) {
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
fun TeamSelectionContent(
    eventType: EventType,
    eventTitle: String,
    homeTeamColor: Int,
    awayTeamColor: Int,
    onTeamSelected: (TeamSelection) -> Unit
) {
    val selectTeamText = stringResource(R.string.title_select_team_generic)
    val eventIconInfo = getEventIconInfo(eventType)
    
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DialogBackgroundColor)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题区域（带图标）
        // Title Area (with icon)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            Icon(
                painter = painterResource(id = eventIconInfo.iconResId),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = eventIconInfo.iconColor
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$eventTitle - $selectTeamText",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // 队伍按钮区域
        // Team Buttons Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 主队按钮 (Home Team Button)
            TeamButton(
                modifier = Modifier.weight(1f),
                teamColor = homeTeamColor,
                iconResId = R.drawable.ic_home,
                onClick = { onTeamSelected(TeamSelection.HOME) }
            )

            // 客队按钮 (Away Team Button)
            TeamButton(
                modifier = Modifier.weight(1f),
                teamColor = awayTeamColor,
                iconResId = R.drawable.ic_flight,
                onClick = { onTeamSelected(TeamSelection.AWAY) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 取消按钮 (Cancel Button)
        Button(
            onClick = { onTeamSelected(TeamSelection.CANCEL) },
            modifier = Modifier.size(60.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = CancelButtonColor
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_close_24),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = CancelIconColor
            )
        }
    }
}

/**
 * 队伍按钮组件
 * Team Button Component
 *
 * @param modifier Modifier
 * @param teamColor 队伍颜色（Int格式）
 * @param iconResId 图标资源ID
 * @param onClick 点击回调
 */
@Composable
fun TeamButton(
    modifier: Modifier = Modifier,
    teamColor: Int,
    iconResId: Int,
    onClick: () -> Unit
) {
    // 判断是否为白色背景，决定图标颜色
    // Determine if background is white to set icon color
    val isWhiteBackground = teamColor == 0xFFFFFFFF.toInt()
    val iconTintColor = if (isWhiteBackground) Color.Black else Color.White

    Button(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(teamColor)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = iconTintColor
        )
    }
}

/**
 * 预览
 * Preview
 */
@Preview(showBackground = true)
@Composable
fun TeamSelectionDialogPreview() {
    TeamSelectionContent(
        eventType = EventType.YELLOW_CARD,
        eventTitle = "Yellow Card",
        homeTeamColor = 0xFF1565C0.toInt(),
        awayTeamColor = 0xFFC62828.toInt(),
        onTeamSelected = {}
    )
}

