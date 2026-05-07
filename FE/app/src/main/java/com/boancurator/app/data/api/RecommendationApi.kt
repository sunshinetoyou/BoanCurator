package com.boancurator.app.data.api

import com.boancurator.app.data.model.CardView
import retrofit2.http.GET
import retrofit2.http.Query

interface RecommendationApi {

    @GET("v1/recommendations")
    suspend fun getRecommendations(
        @Query("n") n: Int = 10
    ): List<CardView>
}
