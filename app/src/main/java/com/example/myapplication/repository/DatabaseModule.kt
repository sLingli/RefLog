package com.example.myapplication.repository

import android.content.Context
import com.example.myapplication.db.RefLogDatabase

/**
 * 数据库依赖提供者（简易 DI）
 *
 * 提供全局单例的 Database 和 Repository 实例。
 * 在 Application 或 MainActivity 中初始化。
 */
object DatabaseModule {

    @Volatile
    private var database: RefLogDatabase? = null

    @Volatile
    private var matchRecordRepo: MatchRecordRepository? = null

    @Volatile
    private var matchTemplateRepo: MatchTemplateRepository? = null

    fun getDatabase(context: Context): RefLogDatabase {
        return database ?: synchronized(this) {
            database ?: RefLogDatabase.getInstance(context).also { database = it }
        }
    }

    fun getMatchRecordRepository(context: Context): MatchRecordRepository {
        return matchRecordRepo ?: synchronized(this) {
            matchRecordRepo ?: MatchRecordRepository(getDatabase(context).matchRecordDao()).also {
                matchRecordRepo = it
            }
        }
    }

    fun getMatchTemplateRepository(context: Context): MatchTemplateRepository {
        return matchTemplateRepo ?: synchronized(this) {
            matchTemplateRepo ?: MatchTemplateRepository(getDatabase(context).matchTemplateDao()).also {
                matchTemplateRepo = it
            }
        }
    }
}
