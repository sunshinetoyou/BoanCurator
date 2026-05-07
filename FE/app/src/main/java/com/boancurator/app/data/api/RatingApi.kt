package com.boancurator.app.data.api

import com.boancurator.app.data.model.Rating
import com.boancurator.app.data.model.RatingRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface RatingApi {

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
}
