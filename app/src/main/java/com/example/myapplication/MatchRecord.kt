package com.example.myapplication

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
    val events: List<String>,
    // 新增：进球详情
    val homeGoals: Int = 0,
    val awayGoals: Int = 0
)
