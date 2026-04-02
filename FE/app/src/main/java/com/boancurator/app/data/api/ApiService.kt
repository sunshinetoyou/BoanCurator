package com.boancurator.app.data.api

import com.boancurator.app.data.model.AuthResponse
import com.boancurator.app.data.model.Bookmark
import com.boancurator.app.data.model.BookmarkView
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.model.GoogleAuthRequest
import com.boancurator.app.data.model.PaginatedResponse
import com.boancurator.app.data.model.User
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // --- Card News ---

    @GET("v1/cardnews")
    suspend fun getCardNews(
        @Query("category") category: String? = null,
        @Query("level") level: String? = null,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): PaginatedResponse

    @GET("v1/cardnews/years")
    suspend fun getAvailableYears(): List<Int>

    // --- Themes ---

    @GET("v1/themes")
    suspend fun getThemes(): List<String>

    // --- Search ---

    @GET("v1/search/semantic")
    suspend fun searchSemantic(
        @Query("q") query: String,
        @Query("n") n: Int = 10
    ): List<CardView>

    @GET("v1/search/theme")
    suspend fun searchByTheme(
        @Query("themes") themes: List<String>,
        @Query("search_type") searchType: Int = 1,
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): PaginatedResponse

    @GET("v1/search/similar/{article_id}")
    suspend fun getSimilarArticles(
        @Path("article_id") articleId: Int,
        @Query("n") n: Int = 10
    ): List<CardView>

    // --- Auth ---

    @POST("v1/auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleAuthRequest
    ): AuthResponse

    // --- User ---

    @GET("v1/users/me")
    suspend fun getCurrentUser(): User

    // --- Bookmarks ---

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

    // --- Recommendations ---

    @GET("v1/recommendations")
    suspend fun getRecommendations(
        @Query("n") n: Int = 10
    ): List<CardView>
}
