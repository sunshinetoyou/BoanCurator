package com.boancurator.app.data.model

import com.google.gson.annotations.SerializedName

data class GoogleAuthRequest(
    val token: String
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("token_type") val tokenType: String,
    val user: User?
)
