package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 赛事预设持久化管理
 *
 * 使用 SharedPreferences + Gson 存储赛事预设列表，
 * 模式与 [MatchRecordManager] 保持一致。
 */
class MatchTemplateManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("match_templates", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_TEMPLATES = "templates"
    }

    /** 保存预设（新增或更新） */
    fun saveTemplate(template: MatchTemplate) {
        val templates = getAllTemplates().toMutableList()
        val index = templates.indexOfFirst { it.id == template.id }
        if (index >= 0) {
            templates[index] = template
        } else {
            templates.add(0, template)
        }
        prefs.edit().putString(KEY_TEMPLATES, gson.toJson(templates)).apply()
        Log.i("TemplateManager", "✅ 预设已保存: ${template.name}，当前总计: ${templates.size} 条")
    }

    /** 获取所有预设 */
    fun getAllTemplates(): List<MatchTemplate> {
        val json = prefs.getString(KEY_TEMPLATES, null) ?: return emptyList()
        val type = object : TypeToken<List<MatchTemplate>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 根据 ID 获取预设 */
    fun getTemplateById(id: Long): MatchTemplate? {
        return getAllTemplates().find { it.id == id }
    }

    /** 删除单条预设 */
    fun deleteTemplate(id: Long) {
        val templates = getAllTemplates().toMutableList()
        templates.removeAll { it.id == id }
        prefs.edit().putString(KEY_TEMPLATES, gson.toJson(templates)).apply()
    }

    /** 清空所有预设 */
    fun clearAllTemplates() {
        prefs.edit().remove(KEY_TEMPLATES).apply()
    }
}
