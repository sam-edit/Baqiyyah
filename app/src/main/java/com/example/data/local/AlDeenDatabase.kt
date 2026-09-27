package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.GoalDao
import com.example.data.local.entities.BookmarkEntity
import com.example.data.local.entities.GoalEntity

@Database(
    entities = [GoalEntity::class, BookmarkEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlDeenDatabase : RoomDatabase() {
    abstract fun goalDao(): GoalDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var INSTANCE: AlDeenDatabase? = null

        fun getInstance(context: Context): AlDeenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlDeenDatabase::class.java,
                    "al_deen.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
