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
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")

        /** JWT는 최소 header.payload.signature (dot 2개) 형태 */
        fun isValidJwt(token: String?): Boolean {
            if (token.isNullOrBlank()) return false
            val parts = token.split(".")
            return parts.size == 3 && parts.all { it.isNotEmpty() }
        }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        val token = prefs[JWT_TOKEN]
        if (isValidJwt(token)) token else null
    }

    suspend fun getToken(): String? {
        val token = context.dataStore.data.first()[JWT_TOKEN]
        return if (isValidJwt(token)) token else null
    }

    suspend fun saveToken(token: String) {
        if (!isValidJwt(token)) return  // 잘못된 토큰 저장 방지
        context.dataStore.edit { prefs ->
            prefs[JWT_TOKEN] = token
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(JWT_TOKEN)
        }
    }
}
