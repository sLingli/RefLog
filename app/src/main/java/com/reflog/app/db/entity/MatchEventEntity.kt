package com.reflog.app.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 比赛事件 Entity — 对应 Room 表 match_events
 *
 * 事件类型用枚举名存储（如 "GOAL"），不依赖本地化字符串。
 * 半场信息同理用 [HalfType] 枚举名。
 */
@Entity(
    tableName = "match_events",
    foreignKeys = [
        ForeignKey(
            entity = MatchRecordEntity::class,
            parentColumns = ["id"],
            childColumns = ["recordId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recordId"])]
)
data class MatchEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recordId: Long,                  // 外键 → match_records.id
    val eventType: String,               // EventType 枚举名，如 "GOAL", "YELLOW_CARD"
    val team: String? = null,            // TeamSelection 枚举名 "HOME"/"AWAY"，伤停/换人为 null
    val playerNumber: String? = null,    // 球衣号码，伤停/换人为 null
    val half: String,                    // HalfType 枚举名 "FIRST_HALF"/"SECOND_HALF"
    val minute: Int = 0,                 // 事件发生的比赛分钟
    val timeSeconds: Long = 0,           // 事件发生的比赛总秒数（用于精确时间显示）
)
