package com.example.data.repository

import com.example.data.local.dao.BookmarkDao
import com.example.data.local.entities.BookmarkEntity
import com.example.domain.model.BookmarkItem
import com.example.domain.model.BookmarkType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookmarksRepository(private val bookmarkDao: BookmarkDao) {

    val allBookmarks: Flow<List<BookmarkItem>> = bookmarkDao.getAllBookmarks().map { entities ->
        entities.map { it.toDomain() }
    }

    fun isBookmarked(referenceId: String): Flow<Boolean> {
        return bookmarkDao.isBookmarked(referenceId)
    }

    suspend fun toggleBookmark(item: BookmarkItem): Boolean {
        // Check if exists
        var exists = false
        // Insert or delete
        try {
            val entity = BookmarkEntity(
                type = item.type.name,
                referenceId = item.referenceId,
                title = item.title,
                subtitle = item.subtitle,
                arabicText = item.arabicText,
                translation = item.translation
            )
            bookmarkDao.insertBookmark(entity)
            return true
        } catch (e: Exception) {
            bookmarkDao.deleteBookmarkByReference(item.referenceId)
            return false
        }
    }

    suspend fun removeBookmark(referenceId: String) {
        bookmarkDao.deleteBookmarkByReference(referenceId)
    }

    suspend fun removeBookmarkById(id: Long) {
        bookmarkDao.deleteBookmarkById(id)
    }

    private fun BookmarkEntity.toDomain(): BookmarkItem {
        return BookmarkItem(
            id = id,
            type = BookmarkType.valueOf(type),
            referenceId = referenceId,
            title = title,
            subtitle = subtitle,
            arabicText = arabicText,
            translation = translation,
            timestamp = timestamp
        )
    }
}
