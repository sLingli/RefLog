package com.example.myapplication

import androidx.compose.ui.graphics.Color

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
 * 判断事件类型是否需要队伍和号码选择
 */
fun EventType.requiresTeamAndNumber(): Boolean {
    return this != EventType.INJURY && this != EventType.SUBSTITUTION
}

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
 * 根据事件类型获取图标信息
 */
fun getEventIconInfo(eventType: EventType): EventIconInfo {
    return when (eventType) {
        EventType.YELLOW_CARD -> EventIconInfo(R.drawable.ic_card, Color.Yellow)
        EventType.RED_CARD -> EventIconInfo(R.drawable.ic_card, Color.Red)
        EventType.GOAL -> EventIconInfo(R.drawable.sports_soccer, Color.White)
        else -> EventIconInfo(R.drawable.ic_card, Color.White)
    }
}
