package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.model.PaginatedResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getCardNews(
        category: String? = null,
        level: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): PaginatedResponse {
        return apiService.getCardNews(category, level, offset, limit)
    }

    suspend fun getThemes(): List<String> {
        return apiService.getThemes()
    }

    suspend fun searchSemantic(query: String): List<CardView> {
        return apiService.searchSemantic(query)
    }

    suspend fun searchByTheme(themes: List<String>, searchType: Int = 1): PaginatedResponse {
        return apiService.searchByTheme(themes, searchType)
    }

    suspend fun getRecommendations(): List<CardView> {
        return apiService.getRecommendations()
    }
}
