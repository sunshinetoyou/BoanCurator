package com.boancurator.app.data.repository

import android.util.Log
import com.boancurator.app.data.api.ApiService
import com.boancurator.app.data.model.FcmTokenRequest
import com.boancurator.app.data.model.GoogleAuthRequest
import com.boancurator.app.data.model.ProfileUpdateRequest
import com.boancurator.app.data.model.ThemesUpdateRequest
import com.boancurator.app.data.model.User
import com.boancurator.app.data.model.UserStats
import com.boancurator.app.util.TokenManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val authStateManager: AuthStateManager,
    private val bookmarkStateHolder: BookmarkStateHolder
) {
    /** 모든 ViewModel은 이 StateFlow를 구독 */
    val isLoggedIn: StateFlow<Boolean> = authStateManager.isLoggedIn

    suspend fun loginWithGoogle(idToken: String): User {
        val authResponse = apiService.loginWithGoogle(GoogleAuthRequest(token = idToken))
        val accessToken = authResponse.accessToken
        val refreshToken = authResponse.refreshToken

        if (!TokenManager.isValidJwt(accessToken)) {
            throw IllegalStateException("서버에서 유효하지 않은 토큰을 반환했습니다")
        }

        tokenManager.saveTokens(accessToken, refreshToken)
        authStateManager.onLogin(accessToken)
        registerFcmToken()
        return authResponse.user ?: apiService.getCurrentUser()
    }

    private suspend fun registerFcmToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            apiService.registerFcmToken(FcmTokenRequest(fcmToken))
            Log.d("Auth", "FCM token registered")
        } catch (e: Exception) {
            Log.e("Auth", "FCM token registration failed", e)
        }
    }

    suspend fun getCurrentUser(): User = apiService.getCurrentUser()

    suspend fun logout() {
        authStateManager.onLogout()  // 즉시 상태 반영
        bookmarkStateHolder.clear()
    }

    suspend fun getUserStats(): UserStats = apiService.getUserStats()

    suspend fun updateProfile(username: String): User =
        apiService.updateProfile(ProfileUpdateRequest(username = username))

    suspend fun getUserExpertise(): Map<String, Double> = apiService.getUserExpertise()

    suspend fun getUserThemes(): List<String> = apiService.getUserThemes()

    suspend fun updateUserThemes(themes: List<String>): List<String> =
        apiService.updateUserThemes(ThemesUpdateRequest(themes))

    suspend fun isAuthenticated(): Boolean = authStateManager.isLoggedIn.value
}
