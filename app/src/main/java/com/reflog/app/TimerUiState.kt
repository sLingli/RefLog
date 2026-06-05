package com.reflog.app

/**
 * 计时器页面 UI 状态
 *
 * 由 [TimerViewModel] 通过 StateFlow 暴露。
 * 显示层文字（statusText、mainTimeColor 等）由 Composable 根据枚举自行派生。
 */
data class TimerUiState(
    val timerState: TimerState = TimerState.READY,
    val currentHalf: HalfState = HalfState.FIRST,
    val mainTimeSeconds: Long = 0,
    val stoppageSeconds: Long = 0,
    val matchName: String = "",
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val homeTeamColor: Int = 0xFF1565C0.toInt(),
    val awayTeamColor: Int = 0xFFC62828.toInt(),
    val showEventSheet: Boolean = false,
    val showEndHalfButton: Boolean = false,
    val matchSummary: MatchSummaryData? = null,
)

/**
 * 比赛总结弹窗数据
 */
data class MatchSummaryData(
    val isHistory: Boolean = false,
    val halfTimeMinutes: Int = 45,
    val homeGoals: Int = 0,
    val awayGoals: Int = 0,
    val yellowCount: Int = 0,
    val redCount: Int = 0,
    val firstHalfStoppage: String = "00:00",
    val secondHalfStoppage: String = "00:00",
    val events: List<MatchEvent> = emptyList(),
)
