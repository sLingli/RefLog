package com.example.myapplication

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
 */

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
            onTeamSelected = onTeamSelected
        )
    }
}

@Composable
fun TeamSelectionContent(
    eventType: EventType,
    eventTitle: String,
    homeTeamColor: Int,
    awayTeamColor: Int,
    onTeamSelected: (TeamSelection) -> Unit
) {
    val eventIconInfo = getEventIconInfo(eventType)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 事件图标
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
                text = eventTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 主队按钮
        TeamButton(
            teamName = stringResource(R.string.team_home),
            teamColor = Color(homeTeamColor),
            onClick = { onTeamSelected(TeamSelection.HOME) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 客队按钮
        TeamButton(
            teamName = stringResource(R.string.team_away),
            teamColor = Color(awayTeamColor),
            onClick = { onTeamSelected(TeamSelection.AWAY) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 取消按钮
        Button(
            onClick = { onTeamSelected(TeamSelection.CANCEL) },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.outline_close_24),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun TeamButton(
    teamName: String,
    teamColor: Color,
    onClick: () -> Unit
) {
    // 判断是否为白色背景，决定文字颜色
    val isWhiteBackground = teamColor == Color.White
    val textColor = if (isWhiteBackground) Color.Black else Color.White

    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = teamColor
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // 颜色圆点
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(teamColor, shape = CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = teamName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

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
