package com.boancurator.app.data.api

import com.boancurator.app.data.model.AuthResponse
import com.boancurator.app.data.model.GoogleAuthRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("v1/auth/google")
    suspend fun loginWithGoogle(
        @Body request: GoogleAuthRequest
    ): AuthResponse
}
