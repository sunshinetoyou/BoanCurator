package com.boancurator.app.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("jwt_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")

        /** JWT는 최소 header.payload.signature (dot 2개) 형태 */
        fun isValidJwt(token: String?): Boolean {
            if (token.isNullOrBlank()) return false
            val parts = token.split(".")
            return parts.size == 3 && parts.all { it.isNotEmpty() }
        }
    }

    /** 로그인 상태 Flow (access token 또는 refresh token이 있으면 로그인 상태) */
    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        val access = prefs[ACCESS_TOKEN]
        val refresh = prefs[REFRESH_TOKEN]
        // access가 있으면 access 반환, 없으면 refresh가 있는지로 로그인 상태 판단
        when {
            isValidJwt(access) -> access
            isValidJwt(refresh) -> "needs_refresh"  // refresh는 있지만 access 만료
            else -> null
        }
    }

    suspend fun getAccessToken(): String? {
        val token = context.dataStore.data.first()[ACCESS_TOKEN]
        return if (isValidJwt(token)) token else null
    }

    suspend fun getRefreshToken(): String? {
        val token = context.dataStore.data.first()[REFRESH_TOKEN]
        return if (isValidJwt(token)) token else null
    }

    /** 하위 호환: 기존 getToken()은 getAccessToken()으로 동작 */
    suspend fun getToken(): String? = getAccessToken()

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            if (isValidJwt(accessToken)) prefs[ACCESS_TOKEN] = accessToken
            if (isValidJwt(refreshToken)) prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    /** Access token만 갱신 (refresh 성공 시) */
    suspend fun updateAccessToken(accessToken: String, refreshToken: String? = null) {
        context.dataStore.edit { prefs ->
            if (isValidJwt(accessToken)) prefs[ACCESS_TOKEN] = accessToken
            if (refreshToken != null && isValidJwt(refreshToken)) {
                prefs[REFRESH_TOKEN] = refreshToken
            }
        }
    }

    /** 하위 호환: 기존 saveToken()은 access token만 저장 */
    suspend fun saveToken(token: String) {
        if (!isValidJwt(token)) return
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = token
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
        }
    }
}
