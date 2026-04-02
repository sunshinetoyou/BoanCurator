package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.Bookmark
import com.boancurator.app.data.model.PaginatedResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getBookmarks(): PaginatedResponse {
        return apiService.getBookmarks()
    }

    suspend fun createBookmark(articleId: Int): Bookmark {
        return apiService.createBookmark(articleId)
    }

    suspend fun deleteBookmark(bookmarkId: Int) {
        apiService.deleteBookmark(bookmarkId)
    }
}
