package com.reflog.app.db

/**
 * 仪表盘聚合统计数据 — 由 SQL 聚合查询直接返回
 */
data class AggregateStats(
    val totalMatches: Int = 0,
    val totalGoals: Int = 0,
    val totalYellowCards: Int = 0,
    val totalRedCards: Int = 0,
)
