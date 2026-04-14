package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.local.ArticleDao
import com.boancurator.app.data.local.ArticleEntity
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.model.PaginatedResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val apiService: ApiService,
    private val articleDao: ArticleDao
) {
    suspend fun getCardNews(
        category: String? = null,
        level: String? = null,
        offset: Int = 0,
        limit: Int = 20
    ): PaginatedResponse {
        val response = apiService.getCardNews(category, level, offset, limit)
        // 캐시에 저장
        articleDao.insertAll(response.items.map { ArticleEntity.from(it) })
        return response
    }

    /** 로컬 캐시에서 가져오기 (오프라인/초기 로드용) */
    suspend fun getCachedArticles(): List<CardView> {
        return articleDao.getAll().map { it.toCardView() }
    }

    suspend fun getCachedByUrl(url: String): CardView? {
        return articleDao.getByUrl(url)?.toCardView()
    }

    suspend fun hasCachedData(): Boolean {
        return articleDao.count() > 0
    }

    suspend fun clearCache() {
        articleDao.deleteAll()
    }

    /** 7일 이상 된 캐시 삭제 */
    suspend fun cleanOldCache() {
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        articleDao.deleteOlderThan(sevenDaysAgo)
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

    suspend fun markArticleRead(articleId: Int) {
        apiService.markArticleRead(articleId)
    }

    suspend fun getRecommendations(): List<CardView> {
        return apiService.getRecommendations()
    }

    suspend fun getAvailableYears(): List<Int> {
        return apiService.getAvailableYears()
    }
}
