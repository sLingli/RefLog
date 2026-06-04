package com.reflog.app.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.reflog.app.db.AggregateStats
import com.reflog.app.db.MatchRecordWithEvents
import com.reflog.app.db.entity.MatchEventEntity
import com.reflog.app.db.entity.MatchRecordEntity

@Dao
interface MatchRecordDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertRecord(record: MatchRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertEvents(events: List<MatchEventEntity>)

    @Transaction
    @Query("SELECT * FROM match_records ORDER BY id DESC")
    suspend fun getAllRecordsWithEvents(): List<MatchRecordWithEvents>

    @Transaction
    @Query("SELECT * FROM match_records ORDER BY id DESC LIMIT :limit")
    suspend fun getRecentRecordsWithEvents(limit: Int): List<MatchRecordWithEvents>

    @Query("DELETE FROM match_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM match_records")
    suspend fun deleteAll()

    /**
     * SQL 聚合查询：直接在数据库层计算仪表盘统计，无需在内存中遍历
     */
    @Query("""
        SELECT
            COUNT(*) AS totalMatches,
            COALESCE(SUM(homeGoals + awayGoals), 0) AS totalGoals,
            COALESCE(SUM(yellowCount), 0) AS totalYellowCards,
            COALESCE(SUM(redCount), 0) AS totalRedCards
        FROM match_records
    """)
    suspend fun getAggregateStats(): AggregateStats

    @Query("SELECT COUNT(*) FROM match_records")
    suspend fun getRecordCount(): Int
}