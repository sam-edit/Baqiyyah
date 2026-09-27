package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_goals",
    indices = [Index(value = ["goalId", "date"], unique = true)]
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goalId: String,
    val date: String, // YYYY-MM-DD
    val isCompleted: Boolean,
    val completedTimestamp: Long? = null
)

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // QURAN_AYAH, HADITH, DUA
    val referenceId: String,
    val title: String,
    val subtitle: String,
    val arabicText: String,
    val translation: String,
    val timestamp: Long = System.currentTimeMillis()
)
