package com.example.myapplication.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.db.dao.MatchRecordDao
import com.example.myapplication.db.dao.MatchTemplateDao
import com.example.myapplication.db.entity.MatchEventEntity
import com.example.myapplication.db.entity.MatchRecordEntity
import com.example.myapplication.db.entity.MatchTemplateEntity

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
