package com.boancurator.app.data.repository

import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.GoogleAuthRequest
import com.boancurator.app.data.model.User
import com.boancurator.app.util.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    val isLoggedIn: Flow<Boolean> = tokenManager.tokenFlow.map { it != null }

    suspend fun loginWithGoogle(idToken: String): User {
        val authResponse = apiService.loginWithGoogle(GoogleAuthRequest(token = idToken))
        val accessToken = authResponse.accessToken
        val refreshToken = authResponse.refreshToken

        if (!TokenManager.isValidJwt(accessToken)) {
            throw IllegalStateException("서버에서 유효하지 않은 토큰을 반환했습니다")
        }

        tokenManager.saveTokens(accessToken, refreshToken)
        return authResponse.user ?: apiService.getCurrentUser()
    }

    suspend fun getCurrentUser(): User {
        return apiService.getCurrentUser()
    }

    suspend fun logout() {
        tokenManager.clearToken()
    }

    suspend fun isAuthenticated(): Boolean {
        return tokenManager.getToken() != null
    }
}
