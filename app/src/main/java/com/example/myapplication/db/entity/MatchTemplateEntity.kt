package com.example.myapplication.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 赛事预设 Entity — 对应 Room 表 match_templates
 */
@Entity(tableName = "match_templates")
data class MatchTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val halfTimeMinutes: Int = 45,
    val homeTeamColor: Int = 0xFF1565C0.toInt(),
    val awayTeamColor: Int = 0xFFC62828.toInt(),
)
