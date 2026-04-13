package com.boancurator.app.ui.screens.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boancurator.app.data.model.Keyword
import com.boancurator.app.data.model.NotificationLog
import com.boancurator.app.data.model.NotificationSettings
import com.boancurator.app.data.model.NotificationSettingsUpdate
import com.boancurator.app.data.model.Source
import com.boancurator.app.data.model.SourceCreateRequest
import com.boancurator.app.data.model.SourceTestResult
import com.boancurator.app.data.model.SourceUpdateRequest
import com.boancurator.app.data.model.User
import com.boancurator.app.data.model.UserStats
import com.boancurator.app.data.repository.AuthRepository
import com.boancurator.app.data.repository.KeywordRepository
import com.boancurator.app.data.repository.NotificationRepository
import com.boancurator.app.data.repository.SourceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val stats: UserStats? = null,
    val expertise: Map<String, Double>? = null,
    val themes: List<String> = emptyList(),
    val keywords: List<Keyword> = emptyList(),
    val notificationSettings: NotificationSettings? = null,
    val notificationLog: List<NotificationLog> = emptyList(),
    val sources: List<Source> = emptyList(),
    val sourceTestResult: SourceTestResult? = null,
    val isSourceTesting: Boolean = false,
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val keywordRepository: KeywordRepository,
    private val notificationRepository: NotificationRepository,
    private val sourceRepository: SourceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
        loadSources()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
                if (loggedIn) {
                    loadUserProfile()
                    loadStats()
                    loadExpertise()
                    loadThemes()
                    loadKeywords()
                    loadNotificationSettings()
                    loadNotificationLog()
                    loadSources()
                } else {
                    _uiState.value = ProfileUiState(isLoggedIn = false, sources = _uiState.value.sources)
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
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun refreshStats() {
        viewModelScope.launch {
            if (!authRepository.isAuthenticated()) return@launch
            loadUserProfile()
            loadStats()
            loadExpertise()
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    // --- Profile ---

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = authRepository.getCurrentUser()
                _uiState.value = _uiState.value.copy(
                    user = user,
                    expertise = user.expertise,
                    error = null,
                )
            } catch (e: HttpException) {
                if (e.code() == 401) {
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

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = authRepository.getUserStats()
                _uiState.value = _uiState.value.copy(stats = stats)
            } catch (_: Exception) {}
        }
    }

    private fun loadExpertise() {
        viewModelScope.launch {
            try {
                val expertise = authRepository.getUserExpertise()
                if (expertise.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(expertise = expertise)
                }
            } catch (_: Exception) {}
        }
    }

    fun updateProfile(username: String) {
        viewModelScope.launch {
            try {
                val updatedUser = authRepository.updateProfile(username)
                _uiState.value = _uiState.value.copy(user = updatedUser)
            } catch (_: Exception) {}
        }
    }

    // --- Themes ---

    private fun loadThemes() {
        viewModelScope.launch {
            try {
                val themes = authRepository.getUserThemes()
                _uiState.value = _uiState.value.copy(themes = themes)
            } catch (_: Exception) {}
        }
    }

    fun toggleTheme(theme: String) {
        val current = _uiState.value.themes.toMutableList()
        if (theme in current) current.remove(theme) else current.add(theme)
        _uiState.value = _uiState.value.copy(themes = current)
        viewModelScope.launch {
            try {
                authRepository.updateUserThemes(current)
            } catch (_: Exception) {
                loadThemes() // rollback
            }
        }
    }

    // --- Keywords ---

    private fun loadKeywords() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(keywords = keywordRepository.getKeywords())
            } catch (_: Exception) {}
        }
    }

    fun createKeyword(keyword: String) {
        viewModelScope.launch {
            try {
                keywordRepository.createKeyword(keyword)
                loadKeywords()
            } catch (_: Exception) {}
        }
    }

    fun deleteKeyword(keywordId: Int) {
        viewModelScope.launch {
            try {
                keywordRepository.deleteKeyword(keywordId)
                loadKeywords()
            } catch (_: Exception) {}
        }
    }

    // --- Notification Settings ---

    private fun loadNotificationSettings() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    notificationSettings = notificationRepository.getSettings()
                )
            } catch (_: Exception) {}
        }
    }

    fun updateNotificationSettings(update: NotificationSettingsUpdate) {
        viewModelScope.launch {
            try {
                val updated = notificationRepository.updateSettings(update)
                _uiState.value = _uiState.value.copy(notificationSettings = updated)
            } catch (_: Exception) {}
        }
    }

    private fun loadNotificationLog() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    notificationLog = notificationRepository.getLog()
                )
            } catch (_: Exception) {}
        }
    }

    // --- Source Management ---

    private fun loadSources() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(sources = sourceRepository.getSources())
            } catch (_: Exception) {}
        }
    }

    fun testSource(url: String) {
        _uiState.value = _uiState.value.copy(isSourceTesting = true, sourceTestResult = null)
        viewModelScope.launch {
            try {
                val result = sourceRepository.testSource(url)
                _uiState.value = _uiState.value.copy(sourceTestResult = result, isSourceTesting = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    sourceTestResult = SourceTestResult(valid = false, message = e.message),
                    isSourceTesting = false
                )
            }
        }
    }

    fun createSource(url: String, sourceName: String?) {
        viewModelScope.launch {
            try {
                sourceRepository.createSource(SourceCreateRequest(url = url, sourceName = sourceName))
                _uiState.value = _uiState.value.copy(sourceTestResult = null)
                loadSources()
            } catch (_: Exception) {}
        }
    }

    fun updateSource(sourceId: Int, request: SourceUpdateRequest) {
        viewModelScope.launch {
            try { sourceRepository.updateSource(sourceId, request); loadSources() }
            catch (_: Exception) {}
        }
    }

    fun deleteSource(sourceId: Int) {
        viewModelScope.launch {
            try { sourceRepository.deleteSource(sourceId); loadSources() }
            catch (_: Exception) {}
        }
    }

    fun clearSourceTestResult() {
        _uiState.value = _uiState.value.copy(sourceTestResult = null)
    }
}
