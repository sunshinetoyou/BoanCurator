package com.boancurator.app.data.api

import com.boancurator.app.data.model.AuthResponse
import com.boancurator.app.data.model.Bookmark
import com.boancurator.app.data.model.BookmarkView
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.model.FcmTokenRequest
import com.boancurator.app.data.model.GoogleAuthRequest
import com.boancurator.app.data.model.Keyword
import com.boancurator.app.data.model.KeywordCreateRequest
import com.boancurator.app.data.model.NotificationLog
import com.boancurator.app.data.model.NotificationSettings
import com.boancurator.app.data.model.NotificationSettingsUpdate
import com.boancurator.app.data.model.PaginatedResponse
import com.boancurator.app.data.model.ProfileUpdateRequest
import com.boancurator.app.data.model.Rating
import com.boancurator.app.data.model.RatingRequest
import com.boancurator.app.data.model.Source
import com.boancurator.app.data.model.SourceCreateRequest
import com.boancurator.app.data.model.SourceListResponse
import com.boancurator.app.data.model.SourceTestResult
import com.boancurator.app.data.model.SourceUpdateRequest
import com.boancurator.app.data.model.ThemesUpdateRequest
import com.boancurator.app.data.model.User
import com.boancurator.app.data.model.UserStats
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
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

    @POST("v1/articles/{article_id}/read")
    suspend fun markArticleRead(
        @Path("article_id") articleId: Int
    )

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

    @GET("v1/users/me/stats")
    suspend fun getUserStats(): UserStats

    @PUT("v1/users/me")
    suspend fun updateProfile(
        @Body request: ProfileUpdateRequest
    ): User

    @GET("v1/users/me/expertise")
    suspend fun getUserExpertise(): Map<String, Double>

    // --- Themes ---

    @GET("v1/users/me/themes")
    suspend fun getUserThemes(): List<String>

    @PUT("v1/users/me/themes")
    suspend fun updateUserThemes(
        @Body request: ThemesUpdateRequest
    ): List<String>

    @DELETE("v1/users/me/themes")
    suspend fun deleteUserThemes()

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

    // --- Ratings ---

    @POST("v1/ratings")
    suspend fun rateArticle(
        @Body request: RatingRequest
    ): Rating

    @GET("v1/ratings")
    suspend fun getRatings(): List<Rating>

    @DELETE("v1/ratings/{article_id}")
    suspend fun deleteRating(
        @Path("article_id") articleId: Int
    )

    // --- Keywords ---

    @POST("v1/keywords")
    suspend fun createKeyword(
        @Body request: KeywordCreateRequest
    ): Keyword

    @GET("v1/keywords")
    suspend fun getKeywords(): List<Keyword>

    @PUT("v1/keywords/{keyword_id}")
    suspend fun updateKeyword(
        @Path("keyword_id") keywordId: Int,
        @Body request: KeywordCreateRequest
    ): Keyword

    @DELETE("v1/keywords/{keyword_id}")
    suspend fun deleteKeyword(
        @Path("keyword_id") keywordId: Int
    )

    // --- Notifications ---

    @GET("v1/notifications/settings")
    suspend fun getNotificationSettings(): NotificationSettings

    @PUT("v1/notifications/settings")
    suspend fun updateNotificationSettings(
        @Body request: NotificationSettingsUpdate
    ): NotificationSettings

    @GET("v1/notifications/log")
    suspend fun getNotificationLog(
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 20
    ): List<NotificationLog>

    // --- FCM ---

    @POST("v1/notifications/fcm-token")
    suspend fun registerFcmToken(
        @Body request: FcmTokenRequest
    )

    // --- Sources ---

    @GET("v1/sources")
    suspend fun getSources(): SourceListResponse

    @POST("v1/sources/test")
    suspend fun testSource(
        @Query("url") url: String
    ): SourceTestResult

    @POST("v1/sources")
    suspend fun createSource(
        @Body request: SourceCreateRequest
    ): Source

    @PUT("v1/sources/{source_id}")
    suspend fun updateSource(
        @Path("source_id") sourceId: Int,
        @Body request: SourceUpdateRequest
    ): Source

    @DELETE("v1/sources/{source_id}")
    suspend fun deleteSource(
        @Path("source_id") sourceId: Int
    )

    // --- Recommendations ---

    @GET("v1/recommendations")
    suspend fun getRecommendations(
        @Query("n") n: Int = 10
    ): List<CardView>
}
