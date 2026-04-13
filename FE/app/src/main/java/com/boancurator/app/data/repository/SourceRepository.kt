package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.Source
import com.boancurator.app.data.model.SourceCreateRequest
import com.boancurator.app.data.model.SourceTestResult
import com.boancurator.app.data.model.SourceUpdateRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SourceRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getSources(): List<Source> {
        val response = apiService.getSources()
        return response.system + response.custom
    }

    suspend fun testSource(url: String): SourceTestResult = apiService.testSource(url)

    suspend fun createSource(request: SourceCreateRequest): Source = apiService.createSource(request)

    suspend fun updateSource(sourceId: Int, request: SourceUpdateRequest): Source =
        apiService.updateSource(sourceId, request)

    suspend fun deleteSource(sourceId: Int) = apiService.deleteSource(sourceId)
}
