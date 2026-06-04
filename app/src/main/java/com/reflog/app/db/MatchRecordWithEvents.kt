package com.reflog.app.db

import androidx.room.Embedded
import androidx.room.Relation
import com.reflog.app.db.entity.MatchEventEntity
import com.reflog.app.db.entity.MatchRecordEntity

/**
 * 比赛记录 + 关联事件列表
 *
 * Room @Transaction 查询自动填充 events 列表。
 */
data class MatchRecordWithEvents(
    @Embedded val record: MatchRecordEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "recordId"
    )
    val events: List<MatchEventEntity>
)
