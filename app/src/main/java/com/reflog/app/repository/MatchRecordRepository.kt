package com.reflog.app.repository

import com.reflog.app.EventType
import com.reflog.app.MatchEvent
import com.reflog.app.MatchRecord
import com.reflog.app.TeamSelection
import com.reflog.app.toEmoji
import com.reflog.app.db.AggregateStats
import com.reflog.app.db.HalfType
import com.reflog.app.db.MatchRecordWithEvents
import com.reflog.app.db.dao.MatchRecordDao
import com.reflog.app.db.entity.MatchEventEntity
import com.reflog.app.db.entity.MatchRecordEntity
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 比赛记录 Repository
 *
 * 封装 [MatchRecordDao]，负责 Entity ↔ data class 的映射转换。
 * 替代旧的 [MatchRecordManager]，使用 Room 实现持久化。
 */
class MatchRecordRepository(private val dao: MatchRecordDao) {

    /**
     * 保存一场比赛记录（含所有事件）
     */
    suspend fun saveRecord(record: MatchRecord) {
        val entityId = dao.insertRecord(record.toEntity())
        val eventEntities = record.events.map { it.toEntity(entityId) }
        dao.insertEvents(eventEntities)
        Log.i("RoomDB", "✅ 比赛记录已保存到 Room (id=$entityId, events=${eventEntities.size})")
    }

    /**
     * 获取所有比赛记录（含事件列表），按时间倒序
     */
    suspend fun getAllRecords(): List<MatchRecord> {
        val records = dao.getAllRecordsWithEvents().map { it.toDataClass() }
        Log.i("RoomDB", "📖 从 Room 读取了 ${records.size} 条比赛记录")
        return records
    }

    /**
     * 获取最近 N 条比赛记录
     */
    suspend fun getRecentRecords(limit: Int): List<MatchRecord> {
        return dao.getRecentRecordsWithEvents(limit).map { it.toDataClass() }
    }

    /**
     * 删除指定比赛记录（级联删除关联事件）
     */
    suspend fun deleteRecord(id: Long) {
        dao.deleteById(id)
    }

    /**
     * 清空所有比赛记录
     */
    suspend fun clearAllRecords() {
        dao.deleteAll()
    }

    /**
     * 获取仪表盘聚合统计（SQL 层计算，不在内存遍历）
     */
    suspend fun getAggregateStats(): AggregateStats {
        return dao.getAggregateStats()
    }

    // ────────────────────────────────────────
    // 映射函数
    // ────────────────────────────────────────

    companion object {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        /**
         * MatchRecord → MatchRecordEntity
         */
        fun MatchRecord.toEntity(): MatchRecordEntity {
            return MatchRecordEntity(
                id = if (id > 0) id else 0,  // Room autoGenerate，旧数据保留原 ID
                date = date,
                halfTimeMinutes = halfTimeMinutes,
                firstHalfStoppageSeconds = parseTimeToSeconds(firstHalfStoppage),
                secondHalfStoppageSeconds = parseTimeToSeconds(secondHalfStoppage),
                homeGoals = homeGoals,
                awayGoals = awayGoals,
                yellowCount = yellowCount,
                redCount = redCount,
                substitutionCount = substitutionCount,
                injuryCount = injuryCount,
                matchName = matchName,
                homeTeamName = homeTeamName,
                awayTeamName = awayTeamName,
                homeTeamColor = homeTeamColor,
                awayTeamColor = awayTeamColor,
            )
        }

        /**
         * MatchEvent → MatchEventEntity
         */
        fun MatchEvent.toEntity(recordId: Long): MatchEventEntity {
            // 尝试从 event 字符串解析回 EventType 枚举
            val eventType = parseEventType(event) ?: EventType.GOAL
            // 尝试从 half 字符串解析回 HalfType 枚举
            val halfType = parseHalfType(half)
            // 尝试从 detail 解析队伍和号码
            val (team, number) = parseDetail(detail)

            return MatchEventEntity(
                recordId = recordId,
                eventType = eventType.name,
                team = team?.name,
                playerNumber = number,
                half = halfType.name,
                minute = minute,
                timeSeconds = parseTimeToSeconds(timeStr),
            )
        }

        /**
         * MatchRecordWithEvents → MatchRecord
         */
        fun MatchRecordWithEvents.toDataClass(): MatchRecord {
            return MatchRecord(
                id = record.id,
                date = record.date,
                halfTimeMinutes = record.halfTimeMinutes,
                firstHalfStoppage = formatSeconds(record.firstHalfStoppageSeconds),
                secondHalfStoppage = formatSeconds(record.secondHalfStoppageSeconds),
                totalStoppage = formatSeconds(record.firstHalfStoppageSeconds + record.secondHalfStoppageSeconds),
                goalCount = record.homeGoals + record.awayGoals,
                yellowCount = record.yellowCount,
                redCount = record.redCount,
                substitutionCount = record.substitutionCount,
                injuryCount = record.injuryCount,
                events = events.map { it.toDataClass() },
                homeGoals = record.homeGoals,
                awayGoals = record.awayGoals,
                matchName = record.matchName,
                homeTeamName = record.homeTeamName,
                awayTeamName = record.awayTeamName,
                homeTeamColor = record.homeTeamColor,
                awayTeamColor = record.awayTeamColor,
            )
        }

        /**
         * MatchEventEntity → MatchEvent
         */
        fun MatchEventEntity.toDataClass(): MatchEvent {
            val eventType = try {
                EventType.valueOf(eventType)
            } catch (_: Exception) {
                EventType.GOAL
            }
            val halfType = try {
                HalfType.valueOf(half)
            } catch (_: Exception) {
                HalfType.FIRST_HALF
            }

            return MatchEvent(
                timeStr = formatSeconds(timeSeconds),
                event = eventType.name,  // 存枚举名，显示时再转本地化
                emoji = eventType.toEmoji(),
                detail = buildDetailString(team, playerNumber),
                half = halfType.name,
                minute = minute,
            )
        }

        // ────────────────────────────────────────
        // 工具函数
        // ────────────────────────────────────────

        private fun formatSeconds(seconds: Long): String {
            val m = seconds / 60
            val s = seconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }

        private fun parseTimeToSeconds(timeStr: String): Long {
            return try {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    parts[0].toLong() * 60 + parts[1].toLong()
                } else 0L
            } catch (_: Exception) {
                0L
            }
        }

        /**
         * 从本地化字符串解析回 EventType 枚举
         * 支持中文和英文
         */
        private fun parseEventType(eventStr: String): EventType? {
            // 先尝试直接解析枚举名（新格式）
            try { return EventType.valueOf(eventStr) } catch (_: Exception) {}
            // 再尝试从本地化字符串解析（旧格式迁移用）
            return when (eventStr) {
                "进球", "Goal" -> EventType.GOAL
                "黄牌", "Yellow Card" -> EventType.YELLOW_CARD
                "红牌", "Red Card" -> EventType.RED_CARD
                "换人", "Sub" -> EventType.SUBSTITUTION
                "受伤", "Injury" -> EventType.INJURY
                else -> null
            }
        }

        /**
         * 从本地化字符串解析回 HalfType 枚举
         */
        private fun parseHalfType(halfStr: String): HalfType {
            try { return HalfType.valueOf(halfStr) } catch (_: Exception) {}
            return when (halfStr) {
                "上半场", "1st Half" -> HalfType.FIRST_HALF
                "下半场", "2nd Half" -> HalfType.SECOND_HALF
                else -> HalfType.FIRST_HALF
            }
        }

        /**
         * 从 detail 字符串解析队伍和号码
         * 旧格式: "主队 #7" / "Home #7" / "客队 #10" / "Away #10"
         */
        private fun parseDetail(detail: String): Pair<TeamSelection?, String?> {
            if (detail.isBlank()) return Pair(null, null)
            val team = when {
                detail.contains("主队") || detail.contains("Home", ignoreCase = true) -> TeamSelection.HOME
                detail.contains("客队") || detail.contains("Away", ignoreCase = true) -> TeamSelection.AWAY
                else -> null
            }
            val number = detail.substringAfter("#").trim().ifBlank { null }
            return Pair(team, number)
        }

        /**
         * 从枚举构建 detail 字符串（用于显示）
         */
        private fun buildDetailString(team: String?, number: String?): String {
            if (team == null && number == null) return ""
            val teamStr = try {
                when (TeamSelection.valueOf(team ?: "")) {
                    TeamSelection.HOME -> "Home"
                    TeamSelection.AWAY -> "Away"
                    TeamSelection.CANCEL -> ""
                }
            } catch (_: Exception) { "" }
            return if (number != null) "$teamStr #$number" else teamStr
        }

        /**
         * 从当前比赛数据构建 MatchRecord（替代旧的 saveMatchRecord 逻辑）
         */
        fun buildMatchRecord(
            matchEvents: List<MatchEvent>,
            halfTimeSeconds: Long,
            firstHalfStoppage: Long,
            stoppageTime: Long,
            currentMatchName: String,
            currentHomeTeamName: String,
            currentAwayTeamName: String,
            homeTeamColor: Int,
            awayTeamColor: Int,
        ): MatchRecord {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            val homeGoals = matchEvents.count { it.eventType() == EventType.GOAL && it.team() == TeamSelection.HOME }
            val awayGoals = matchEvents.count { it.eventType() == EventType.GOAL && it.team() == TeamSelection.AWAY }

            return MatchRecord(
                date = currentDate,
                halfTimeMinutes = (halfTimeSeconds / 60).toInt(),
                firstHalfStoppage = formatSeconds(firstHalfStoppage),
                secondHalfStoppage = formatSeconds(stoppageTime),
                totalStoppage = formatSeconds(firstHalfStoppage + stoppageTime),
                goalCount = matchEvents.count { it.eventType() == EventType.GOAL },
                yellowCount = matchEvents.count { it.eventType() == EventType.YELLOW_CARD },
                redCount = matchEvents.count { it.eventType() == EventType.RED_CARD },
                substitutionCount = matchEvents.count { it.eventType() == EventType.SUBSTITUTION },
                injuryCount = matchEvents.count { it.eventType() == EventType.INJURY },
                events = matchEvents,
                homeGoals = homeGoals,
                awayGoals = awayGoals,
                matchName = currentMatchName,
                homeTeamName = currentHomeTeamName,
                awayTeamName = currentAwayTeamName,
                homeTeamColor = homeTeamColor,
                awayTeamColor = awayTeamColor,
            )
        }

        /**
         * MatchEvent 的辅助扩展：从 event 字段解析 EventType
         */
        private fun MatchEvent.eventType(): EventType {
            return parseEventType(event) ?: EventType.GOAL
        }

        /**
         * MatchEvent 的辅助扩展：从 detail 字段解析 TeamSelection
         */
        private fun MatchEvent.team(): TeamSelection? {
            return parseDetail(detail).first
        }
    }
}
