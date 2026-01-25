package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MatchRecordManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("match_records", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_RECORDS = "records"
    }

    // 保存比赛记录
    fun saveRecord(record: MatchRecord) {
        val records = getAllRecords().toMutableList()
        records.add(0, record)  // 新记录添加到最前面

        // 最多保留50条记录
        if (records.size > 50) {
            records.removeAt(records.size - 1)
        }

        val json = gson.toJson(records)
        prefs.edit().putString(KEY_RECORDS, json).apply()
    }

    // 获取所有记录
    fun getAllRecords(): List<MatchRecord> {
        val json = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        val type = object : TypeToken<List<MatchRecord>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 清空所有记录
    fun clearAllRecords() {
        prefs.edit().remove(KEY_RECORDS).apply()
    }

    // 删除单条记录
    fun deleteRecord(id: Long) {
        val records = getAllRecords().toMutableList()
        records.removeAll { it.id == id }
        val json = gson.toJson(records)
        prefs.edit().putString(KEY_RECORDS, json).apply()
    }
}
