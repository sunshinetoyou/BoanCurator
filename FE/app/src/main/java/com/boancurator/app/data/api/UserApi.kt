package com.boancurator.app.data.api

import com.boancurator.app.data.model.ProfileUpdateRequest
import com.boancurator.app.data.model.ThemesUpdateRequest
import com.boancurator.app.data.model.User
import com.boancurator.app.data.model.UserStats
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT

interface UserApi {

    @GET("v1/users/me")
    suspend fun getCurrentUser(): User

    @GET("v1/users/me/stats")
    suspend fun getUserStats(): UserStats

    @PUT("v1/users/me")
    suspend fun updateProfile(
        @Body request: ProfileUpdateRequest
    ): User

    @GET("v1/users/me/expertise")
    suspend fun getUserExpertise(): Map<String, Double>

    @GET("v1/users/me/themes")
    suspend fun getUserThemes(): List<String>

    @PUT("v1/users/me/themes")
    suspend fun updateUserThemes(
        @Body request: ThemesUpdateRequest
    ): List<String>

    @DELETE("v1/users/me/themes")
    suspend fun deleteUserThemes()
}
