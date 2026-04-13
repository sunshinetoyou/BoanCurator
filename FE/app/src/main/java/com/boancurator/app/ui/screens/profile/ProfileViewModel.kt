package com.boancurator.app.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.model.User
import com.boancurator.app.data.repository.ArticleRepository
import com.boancurator.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val recommendations: List<CardView> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
                if (loggedIn) {
                    loadUserProfile()
                    loadRecommendations()
                } else {
                    _uiState.value = _uiState.value.copy(user = null, recommendations = emptyList())
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val user = authRepository.loginWithGoogle(idToken)
                _uiState.value = _uiState.value.copy(
                    user = user, isLoggedIn = true, isLoading = false, error = null
                )
                loadRecommendations()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = ProfileUiState()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                _uiState.value = _uiState.value.copy(user = user, error = null)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    // TokenAuthenticator가 refresh 시도 후에도 401이면 세션 만료
                    Log.w("ProfileVM", "Session expired, logging out")
                    authRepository.logout()
                    _uiState.value = ProfileUiState(error = "세션이 만료되었습니다. 다시 로그인해주세요.")
                } else {
                    Log.e("ProfileVM", "Failed to load profile: ${e.code()}", e)
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Failed to load profile", e)
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                val recs = articleRepository.getRecommendations()
                _uiState.value = _uiState.value.copy(recommendations = recs)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    Log.w("ProfileVM", "Session expired during recommendations load")
                    // loadUserProfile에서 이미 처리됨
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Failed to load recommendations", e)
            }
        }
    }
}
