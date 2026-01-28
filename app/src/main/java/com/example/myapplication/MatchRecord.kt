package com.example.myapplication

// 1. 先定义“事件”的格式
data class MatchEvent(
    val timeStr: String,
    val event: String,
    val emoji: String = "",
    val detail: String = "",
    val half: String = "",
    val minute: Int = 0
)

// 2. 再定义“整场比赛记录”的格式
data class MatchRecord(
    val id: Long = System.currentTimeMillis(),
    val date: String,
    val halfTimeMinutes: Int,
    val firstHalfStoppage: String,
    val secondHalfStoppage: String,
    val totalStoppage: String,
    val goalCount: Int,
    val yellowCount: Int,
    val redCount: Int,
    val substitutionCount: Int,
    val injuryCount: Int,
    val events: List<MatchEvent>,
    val homeGoals: Int = 0,
    val awayGoals: Int = 0
)
