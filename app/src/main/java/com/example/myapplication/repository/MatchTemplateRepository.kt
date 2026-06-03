package com.example.myapplication.repository

import com.example.myapplication.MatchTemplate
import com.example.myapplication.db.dao.MatchTemplateDao
import com.example.myapplication.db.entity.MatchTemplateEntity

/**
 * 赛事预设 Repository
 *
 * 封装 [MatchTemplateDao]，替代旧的 [MatchTemplateManager]。
 */
class MatchTemplateRepository(private val dao: MatchTemplateDao) {

    /**
     * 保存预设（新增或更新）
     */
    suspend fun saveTemplate(template: MatchTemplate) {
        val entity = template.toEntity()
        if (entity.id > 0 && dao.getById(entity.id) != null) {
            dao.update(entity)
        } else {
            dao.insert(entity)
        }
    }

    /**
     * 获取所有预设
     */
    suspend fun getAllTemplates(): List<MatchTemplate> {
        return dao.getAll().map { it.toDataClass() }
    }

    /**
     * 删除指定预设
     */
    suspend fun deleteTemplate(id: Long) {
        dao.deleteById(id)
    }

    /**
     * 清空所有预设
     */
    suspend fun clearAllTemplates() {
        dao.deleteAll()
    }

    // ────────────────────────────────────────
    // 映射函数
    // ────────────────────────────────────────

    private fun MatchTemplate.toEntity(): MatchTemplateEntity {
        return MatchTemplateEntity(
            id = id,
            name = name,
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            halfTimeMinutes = halfTimeMinutes,
            homeTeamColor = homeTeamColor,
            awayTeamColor = awayTeamColor,
        )
    }

    private fun MatchTemplateEntity.toDataClass(): MatchTemplate {
        return MatchTemplate(
            id = id,
            name = name,
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            halfTimeMinutes = halfTimeMinutes,
            homeTeamColor = homeTeamColor,
            awayTeamColor = awayTeamColor,
        )
    }
}
