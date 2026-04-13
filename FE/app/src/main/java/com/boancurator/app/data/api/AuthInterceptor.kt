package com.boancurator.app.data.api

import com.boancurator.app.data.repository.AuthStateManager
import com.boancurator.app.util.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager,
    private val authStateManager: AuthStateManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // 토큰이 있으면 헤더에 추가
        val token = runBlocking { tokenManager.getToken() }
        val finalRequest = if (token != null) {
            request.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(finalRequest)

        // 401 → 토큰 무효 → 상태 업데이트 (비동기, 블로킹 안 함)
        if (response.code == 401 && token != null) {
            runBlocking { authStateManager.onTokenInvalid() }
        }

        return response
    }
}
