package com.example.myapplication.repository

import android.content.Context
import android.util.Log
import com.example.myapplication.MatchRecord
import com.example.myapplication.MatchTemplate
import com.example.myapplication.db.RefLogDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SharedPreferences → Room 一次性迁移工具
 *
 * 首次启动时检测旧数据，迁移到 Room 数据库，迁移完成后清除旧 SharedPreferences。
 * 使用 "match_records" SharedPreferences 中是否存在 "records" key 来判断是否需要迁移。
 */
object MigrationHelper {

    private const val TAG = "MigrationHelper"

    /**
     * 如果存在旧数据则执行迁移
     *
     * 应在 Application.onCreate 或 MainActivity.onCreate 中调用，
     * 在任何数据库操作之前执行。
     */
    suspend fun migrateIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val oldRecordsPrefs = context.getSharedPreferences("match_records", Context.MODE_PRIVATE)
        val oldTemplatesPrefs = context.getSharedPreferences("match_templates", Context.MODE_PRIVATE)

        val hasOldRecords = oldRecordsPrefs.contains("records")
        val hasOldTemplates = oldTemplatesPrefs.contains("templates")

        if (!hasOldRecords && !hasOldTemplates) {
            Log.i(TAG, "无需迁移，无旧数据")
            return@withContext
        }

        Log.i(TAG, "开始迁移旧数据...")
        val db = RefLogDatabase.getInstance(context)
        val recordDao = db.matchRecordDao()
        val templateDao = db.matchTemplateDao()
        val gson = Gson()

        // 迁移比赛记录
        if (hasOldRecords) {
            try {
                val json = oldRecordsPrefs.getString("records", null)
                if (json != null) {
                    val type = object : TypeToken<List<MatchRecord>>() {}.type
                    val oldRecords: List<MatchRecord> = gson.fromJson(json, type)

                    val recordRepo = MatchRecordRepository(recordDao)
                    for (record in oldRecords) {
                        recordRepo.saveRecord(record)
                    }
                    Log.i(TAG, "✅ 比赛记录迁移完成: ${oldRecords.size} 条")
                }
                // 清除旧数据
                oldRecordsPrefs.edit().remove("records").apply()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 比赛记录迁移失败", e)
            }
        }

        // 迁移赛事预设
        if (hasOldTemplates) {
            try {
                val json = oldTemplatesPrefs.getString("templates", null)
                if (json != null) {
                    val type = object : TypeToken<List<MatchTemplate>>() {}.type
                    val oldTemplates: List<MatchTemplate> = gson.fromJson(json, type)

                    val templateRepo = MatchTemplateRepository(templateDao)
                    for (template in oldTemplates) {
                        templateRepo.saveTemplate(template)
                    }
                    Log.i(TAG, "✅ 赛事预设迁移完成: ${oldTemplates.size} 条")
                }
                // 清除旧数据
                oldTemplatesPrefs.edit().remove("templates").apply()
            } catch (e: Exception) {
                Log.e(TAG, "❌ 赛事预设迁移失败", e)
            }
        }

        Log.i(TAG, "数据迁移结束")
    }
}
