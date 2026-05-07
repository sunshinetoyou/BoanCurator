package com.boancurator.app.data.api

import com.boancurator.app.data.model.Source
import com.boancurator.app.data.model.SourceCreateRequest
import com.boancurator.app.data.model.SourceListResponse
import com.boancurator.app.data.model.SourceTestResult
import com.boancurator.app.data.model.SourceUpdateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SourceApi {

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
}
