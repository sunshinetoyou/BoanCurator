package com.boancurator.app.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.repository.ArticleRepository
import com.boancurator.app.data.repository.AuthRepository
import com.boancurator.app.data.repository.BookmarkRepository
import com.boancurator.app.data.repository.BookmarkStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FeedUiState(
    val articles: List<CardView> = emptyList(),
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val authRepository: AuthRepository,
    private val bookmarkRepository: BookmarkRepository,
    val bookmarkState: BookmarkStateHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(FeedUiState())
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                _uiState.value = _uiState.value.copy(isLoggedIn = loggedIn)
                if (loggedIn) {
                    loadRecommendations()
                } else {
                    _uiState.value = _uiState.value.copy(articles = emptyList())
                }
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                authRepository.loginWithGoogle(idToken)
                // observeAuthState가 자동으로 추천 로드
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun toggleBookmark(article: CardView) {
        val articleId = article.articleId ?: return
        val url = article.url ?: return
        val existingId = bookmarkState.getBookmarkId(url)

        if (existingId != null) {
            bookmarkState.remove(url)
            viewModelScope.launch {
                try { bookmarkRepository.deleteBookmark(existingId) }
                catch (e: Exception) { bookmarkState.add(url, existingId) }
            }
        } else {
            bookmarkState.add(url, -1)
            viewModelScope.launch {
                try {
                    val bm = bookmarkRepository.createBookmark(articleId)
                    bookmarkState.add(url, bm.id)
                } catch (e: Exception) { bookmarkState.remove(url) }
            }
        }
    }

    private fun loadRecommendations() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val recs = articleRepository.getRecommendations()
                _uiState.value = _uiState.value.copy(
                    articles = recs,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                // 추천 실패 시 최신 기사로 fallback
                try {
                    val response = articleRepository.getCardNews(limit = 10)
                    _uiState.value = _uiState.value.copy(
                        articles = response.items,
                        isLoading = false
                    )
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
}
