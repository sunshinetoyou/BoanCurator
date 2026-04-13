package com.boancurator.app.data.api

import android.util.Log
import com.boancurator.app.BuildConfig
import com.boancurator.app.util.TokenManager
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

private data class RefreshRequestBody(
    @SerializedName("refresh_token") val refreshToken: String
)

private data class RefreshResponseBody(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String
)

/**
 * OkHttp Authenticator: 401 응답 시 refresh token으로 자동 토큰 갱신.
 * 갱신 실패 시 토큰을 클리어하여 로그아웃 상태로 전환.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager
) : Authenticator {

    private val gson = Gson()
    // refresh 요청에는 AuthInterceptor를 붙이지 않는 별도 클라이언트 사용
    private val refreshClient = OkHttpClient()

    override fun authenticate(route: Route?, response: Response): Request? {
        // 이미 refresh 시도한 요청이면 무한루프 방지
        if (response.request.header("X-Refresh-Attempted") != null) {
            runBlocking { tokenManager.clearToken() }
            return null
        }

        val refreshToken = runBlocking { tokenManager.getRefreshToken() } ?: run {
            runBlocking { tokenManager.clearToken() }
            return null
        }

        // Refresh 요청
        val refreshBody = gson.toJson(RefreshRequestBody(refreshToken))
        val refreshRequest = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}/v1/auth/refresh")
            .post(refreshBody.toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val refreshResponse = refreshClient.newCall(refreshRequest).execute()

            if (refreshResponse.isSuccessful) {
                val body = refreshResponse.body?.string()
                val tokens = gson.fromJson(body, RefreshResponseBody::class.java)

                runBlocking {
                    tokenManager.updateAccessToken(tokens.accessToken, tokens.refreshToken)
                }

                Log.d("TokenAuthenticator", "Token refreshed successfully")

                // 원래 요청을 새 access token으로 재시도
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${tokens.accessToken}")
                    .header("X-Refresh-Attempted", "true")
                    .build()
            } else {
                Log.w("TokenAuthenticator", "Refresh failed: ${refreshResponse.code}")
                runBlocking { tokenManager.clearToken() }
                null
            }
        } catch (e: Exception) {
            Log.e("TokenAuthenticator", "Refresh error", e)
            runBlocking { tokenManager.clearToken() }
            null
        }
    }
}
