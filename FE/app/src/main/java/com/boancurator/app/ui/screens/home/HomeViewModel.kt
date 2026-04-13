package com.boancurator.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.repository.ArticleRepository
import com.boancurator.app.data.repository.AuthRepository
import com.boancurator.app.data.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// === 보안 분야 매핑 (ArticleCard에서 참조) ===
enum class SecurityField(val label: String) {
    GENERAL("보안 일반"),
    AI("AI 보안"),
    INFRA("인프라 보안"),
    DEV("개발 보안"),
    POLICY("보안 정책"),
    ETC("ETC")
}

fun getSecurityField(themes: List<String>?): SecurityField {
    if (themes.isNullOrEmpty() || "Security" !in themes) return SecurityField.ETC
    return when {
        "AI/ML" in themes -> SecurityField.AI
        "Infra/Cloud" in themes -> SecurityField.INFRA
        "Development" in themes -> SecurityField.DEV
        "Business/Policy" in themes -> SecurityField.POLICY
        else -> SecurityField.GENERAL
    }
}

// === UI State ===
data class HomeUiState(
    val articles: List<CardView> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarkMap: Map<String, Int> = emptyMap()
) {
    val bookmarkedUrls: Set<String> get() = bookmarkMap.keys
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var _isAuthenticated = false

    companion object {
        private const val FEED_SIZE = 10
    }

    init {
        refreshAuthStatus()
        loadFeed()
        loadBookmarkState()
    }

    fun refresh() {
        loadFeed()
        loadBookmarkState()
    }

    private fun loadFeed() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // 캐시 먼저 표시
                val cached = try { articleRepository.getCachedArticles() } catch (_: Exception) { emptyList() }
                if (cached.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        articles = cached.sortedByDescending { it.publishedAt }.take(FEED_SIZE),
                        isLoading = false
                    )
                }

                // API에서 최신 데이터
                val response = articleRepository.getCardNews(offset = 0, limit = FEED_SIZE)
                _uiState.value = _uiState.value.copy(
                    articles = response.items,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.articles.isEmpty()) e.message else null
                )
            }
        }
    }

    private fun loadBookmarkState() {
        viewModelScope.launch {
            try {
                val authenticated = authRepository.isAuthenticated()
                _isAuthenticated = authenticated
                if (!authenticated) return@launch
                val bookmarks = bookmarkRepository.getBookmarks()
                val map = bookmarks.associate { it.url to it.bookmarkId }
                _uiState.value = _uiState.value.copy(bookmarkMap = map)
            } catch (_: Exception) {}
        }
    }

    private fun refreshAuthStatus() {
        viewModelScope.launch {
            _isAuthenticated = authRepository.isAuthenticated()
        }
    }

    /** @return true if bookmark toggled, false if not logged in */
    fun toggleBookmark(article: CardView): Boolean {
        if (!_isAuthenticated) return false
        val articleId = article.articleId ?: return false

        val currentMap = _uiState.value.bookmarkMap
        val existingBookmarkId = currentMap[article.url]

        if (existingBookmarkId != null) {
            _uiState.value = _uiState.value.copy(bookmarkMap = currentMap - article.url)
            viewModelScope.launch {
                try {
                    bookmarkRepository.deleteBookmark(existingBookmarkId)
                } catch (e: Exception) {
                    Log.e("HomeVM", "Bookmark delete failed", e)
                    _uiState.value = _uiState.value.copy(
                        bookmarkMap = _uiState.value.bookmarkMap + (article.url to existingBookmarkId)
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(bookmarkMap = currentMap + (article.url to -1))
            viewModelScope.launch {
                try {
                    val bookmark = bookmarkRepository.createBookmark(articleId)
                    _uiState.value = _uiState.value.copy(
                        bookmarkMap = _uiState.value.bookmarkMap + (article.url to bookmark.id)
                    )
                } catch (e: Exception) {
                    Log.e("HomeVM", "Bookmark create failed", e)
                    _uiState.value = _uiState.value.copy(
                        bookmarkMap = _uiState.value.bookmarkMap - article.url
                    )
                }
            }
        }
        return true
    }
}
