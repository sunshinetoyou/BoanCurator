package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.Rating
import com.boancurator.app.data.model.RatingRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RatingRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun rateArticle(articleId: Int, rating: Int): Rating =
        apiService.rateArticle(RatingRequest(articleId, rating))

    suspend fun getRatings(): List<Rating> = apiService.getRatings()

    suspend fun deleteRating(articleId: Int) = apiService.deleteRating(articleId)
}
