package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.Bookmark
import com.boancurator.app.data.model.BookmarkView
import com.boancurator.app.data.model.CardView
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getBookmarks(): List<BookmarkView> {
        return apiService.getBookmarks()
    }

    suspend fun getBookmarkedCards(): List<CardView> {
        return apiService.getBookmarks().map { it.toCardView() }
    }

    suspend fun createBookmark(articleId: Int): Bookmark {
        return apiService.createBookmark(articleId)
    }

    suspend fun deleteBookmark(bookmarkId: Int) {
        apiService.deleteBookmark(bookmarkId)
    }
}
