package com.reflog.app

/**
 * 仪表盘页面状态
 *
 * 封装 Dashboard 需要的所有派生数据，
 * 由 [DashboardViewModel] 从历史记录聚合计算而来。
 */
data class DashboardState(
    val totalMatches: Int = 0,
    val totalGoals: Int = 0,
    val totalYellowCards: Int = 0,
    val totalRedCards: Int = 0,
    val recentRecords: List<MatchRecord> = emptyList(),
)
