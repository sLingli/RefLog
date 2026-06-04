package com.reflog.app.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.reflog.app.db.entity.MatchTemplateEntity

@Dao
interface MatchTemplateDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(template: MatchTemplateEntity): Long

    @Update
    suspend fun update(template: MatchTemplateEntity)

    @Query("SELECT * FROM match_templates ORDER BY id DESC")
    suspend fun getAll(): List<MatchTemplateEntity>

    @Query("SELECT * FROM match_templates WHERE id = :id")
    suspend fun getById(id: Long): MatchTemplateEntity?

    @Query("DELETE FROM match_templates WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM match_templates")
    suspend fun deleteAll()
}