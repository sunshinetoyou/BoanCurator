package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.Keyword
import com.boancurator.app.data.model.KeywordCreateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getKeywords(): List<Keyword> = apiService.getKeywords()

    suspend fun createKeyword(keyword: String): Keyword =
        apiService.createKeyword(KeywordCreateRequest(keyword))

    suspend fun deleteKeyword(keywordId: Int) = apiService.deleteKeyword(keywordId)
}
