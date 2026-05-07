package com.boancurator.app.data.api

import com.boancurator.app.data.model.Bookmark
import com.boancurator.app.data.model.BookmarkView
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookmarkApi {

    @POST("v1/bookmarks")
    suspend fun createBookmark(
        @Query("article_id") articleId: Int
    ): Bookmark

    @GET("v1/bookmarks")
    suspend fun getBookmarks(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<BookmarkView>

    @DELETE("v1/bookmarks/{bookmark_id}")
    suspend fun deleteBookmark(
        @Path("bookmark_id") bookmarkId: Int
    )
}
