package com.boancurator.app.data.repository

import com.boancurator.app.util.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 전체에서 공유하는 인증 상태 관리자.
 * - StateFlow로 thread-safe 보장
 * - 토큰 저장/삭제 후 즉시 상태 반영
 * - 모든 ViewModel이 이 하나의 소스를 구독
 */
@Singleton
class AuthStateManager @Inject constructor(
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        // DataStore에서 초기 토큰 로드 + 이후 변경 감지
        scope.launch {
            tokenManager.tokenFlow.collect { token ->
                _isLoggedIn.value = token != null
            }
        }
    }

    /** 토큰 저장 후 즉시 상태 업데이트 (동기적으로 보장) */
    suspend fun onLogin(token: String) {
        tokenManager.saveToken(token)
        // DataStore Flow 전파를 기다리지 않고 즉시 상태 반영
        _isLoggedIn.value = TokenManager.isValidJwt(token)
    }

    /** 토큰 삭제 후 즉시 상태 업데이트 */
    suspend fun onLogout() {
        _isLoggedIn.value = false  // 즉시 반영
        tokenManager.clearToken()
    }

    /** 401 응답 시 호출 — 이미 로그아웃 상태면 무시 */
    suspend fun onTokenInvalid() {
        if (_isLoggedIn.value) {
            _isLoggedIn.value = false
            tokenManager.clearToken()
        }
    }

    /** 초기 로딩 완료 대기 (앱 시작 시) */
    suspend fun awaitInitialState(): Boolean {
        return tokenManager.tokenFlow.first() != null
    }
}
