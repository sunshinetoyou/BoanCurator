package com.boancurator.app.data.api

import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.model.PaginatedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SearchApi {

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
}
