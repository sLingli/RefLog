package com.reflog.app.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 比赛记录 Entity — 对应 Room 表 match_records
 *
 * 只存储标量字段；事件列表通过 [MatchEventEntity] 外键关联。
 */
@Entity(tableName = "match_records")
data class MatchRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,                    // "yyyy-MM-dd HH:mm"
    val halfTimeMinutes: Int,
    val firstHalfStoppageSeconds: Long,
    val secondHalfStoppageSeconds: Long,
    val homeGoals: Int = 0,
    val awayGoals: Int = 0,
    val yellowCount: Int = 0,
    val redCount: Int = 0,
    val substitutionCount: Int = 0,
    val injuryCount: Int = 0,
    val matchName: String = "",
    val homeTeamName: String = "",
    val awayTeamName: String = "",
    val homeTeamColor: Int = 0xFF1565C0.toInt(),
    val awayTeamColor: Int = 0xFFC62828.toInt(),
)
