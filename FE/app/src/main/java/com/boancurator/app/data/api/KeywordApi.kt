package com.boancurator.app.data.api

import com.boancurator.app.data.model.Keyword
import com.boancurator.app.data.model.KeywordCreateRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface KeywordApi {

    @POST("v1/keywords")
    suspend fun createKeyword(
        @Body request: KeywordCreateRequest
    ): Keyword

    @GET("v1/keywords")
    suspend fun getKeywords(): List<Keyword>

    @PUT("v1/keywords/{keyword_id}")
    suspend fun updateKeyword(
        @Path("keyword_id") keywordId: Int,
        @Body request: KeywordCreateRequest
    ): Keyword

    @DELETE("v1/keywords/{keyword_id}")
    suspend fun deleteKeyword(
        @Path("keyword_id") keywordId: Int
    )
}
