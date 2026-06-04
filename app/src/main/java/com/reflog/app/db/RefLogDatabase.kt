package com.reflog.app.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.reflog.app.db.dao.MatchRecordDao
import com.reflog.app.db.dao.MatchTemplateDao
import com.reflog.app.db.entity.MatchEventEntity
import com.reflog.app.db.entity.MatchRecordEntity
import com.reflog.app.db.entity.MatchTemplateEntity

@Database(
    entities = [
        MatchRecordEntity::class,
        MatchEventEntity::class,
        MatchTemplateEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class RefLogDatabase : RoomDatabase() {

    abstract fun matchRecordDao(): MatchRecordDao
    abstract fun matchTemplateDao(): MatchTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: RefLogDatabase? = null

        fun getInstance(context: Context): RefLogDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RefLogDatabase::class.java,
                    "reflog.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
