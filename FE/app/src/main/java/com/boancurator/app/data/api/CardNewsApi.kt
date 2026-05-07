package com.boancurator.app.data.api

import com.boancurator.app.data.model.PaginatedResponse
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface CardNewsApi {

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

    @GET("v1/themes")
    suspend fun getThemes(): List<String>
}
