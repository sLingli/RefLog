package com.example.myapplication

/**
 * 赛事预设数据模型
 *
 * 存储一场比赛的配置信息，用于快速开始比赛。
 * 通过 [MatchTemplateManager] 进行持久化管理。
 */
data class MatchTemplate(
    val id: Long = System.currentTimeMillis(),
    val name: String = "",                  // 赛事名称，如"联赛第5轮"
    val homeTeamName: String = "",          // 主队名
    val awayTeamName: String = "",          // 客队名
    val halfTimeMinutes: Int = 45,          // 半场时长（分钟）
    val homeTeamColor: Int = 0xFF1565C0.toInt(),  // 主队球衣主色（默认蓝）
    val awayTeamColor: Int = 0xFFC62828.toInt(),  // 客队球衣主色（默认红）
)
