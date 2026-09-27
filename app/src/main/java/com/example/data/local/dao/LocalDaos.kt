package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.BookmarkEntity
import com.example.data.local.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM daily_goals WHERE date = :date")
    fun getGoalsForDate(date: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM daily_goals WHERE date = :date")
    suspend fun getGoalsForDateSync(date: String): List<GoalEntity>

    @Query("SELECT * FROM daily_goals WHERE date >= :startDate AND date <= :endDate")
    suspend fun getGoalsBetweenDates(startDate: String, endDate: String): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGoal(goal: GoalEntity)

    @Query("DELETE FROM daily_goals WHERE goalId = :goalId AND date = :date")
    suspend fun deleteGoal(goalId: String, date: String)

    @Query("DELETE FROM daily_goals WHERE date < :cutoffDate")
    suspend fun deleteGoalsOlderThan(cutoffDate: String): Int
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE type = :type ORDER BY timestamp DESC")
    fun getBookmarksByType(type: String): Flow<List<BookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE referenceId = :referenceId LIMIT 1)")
    fun isBookmarked(referenceId: String): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Query("DELETE FROM bookmarks WHERE referenceId = :referenceId")
    suspend fun deleteBookmarkByReference(referenceId: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)
}
