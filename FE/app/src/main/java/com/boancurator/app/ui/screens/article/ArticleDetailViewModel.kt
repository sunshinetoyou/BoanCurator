package com.boancurator.app.ui.screens.article

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boancurator.app.data.repository.ArticleRepository
import com.boancurator.app.data.repository.AuthRepository
import com.boancurator.app.data.repository.BookmarkRepository
import com.boancurator.app.data.repository.BookmarkStateHolder
import com.boancurator.app.data.repository.RatingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val authRepository: AuthRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val ratingRepository: RatingRepository,
    val bookmarkState: BookmarkStateHolder
) : ViewModel() {

    private var _hasMarkedRead = false

    private val _currentRating = MutableStateFlow<Int?>(null)
    val currentRating: StateFlow<Int?> = _currentRating

    private val isLoggedIn: Boolean get() = authRepository.isLoggedIn.value

    fun markAsRead(articleId: Int) {
        if (!isLoggedIn || _hasMarkedRead || articleId <= 0) return
        _hasMarkedRead = true
        viewModelScope.launch {
            try { articleRepository.markArticleRead(articleId) }
            catch (_: Exception) {}
        }
    }

    fun loadRating(articleId: Int) {
        if (!isLoggedIn || articleId <= 0) return
        viewModelScope.launch {
            try {
                val ratings = ratingRepository.getRatings()
                _currentRating.value = ratings.find { it.articleId == articleId }?.rating
            } catch (_: Exception) {}
        }
    }

    fun rateArticle(articleId: Int, rating: Int): Boolean {
        if (!isLoggedIn || articleId <= 0) return false
        val current = _currentRating.value
        if (current == rating) {
            _currentRating.value = null
            viewModelScope.launch {
                try { ratingRepository.deleteRating(articleId) }
                catch (_: Exception) { _currentRating.value = current }
            }
        } else {
            _currentRating.value = rating
            viewModelScope.launch {
                try { ratingRepository.rateArticle(articleId, rating) }
                catch (_: Exception) { _currentRating.value = current }
            }
        }
        return true
    }

    fun toggleBookmark(articleId: Int, url: String): Boolean {
        if (!isLoggedIn) return false
        val existingId = bookmarkState.getBookmarkId(url)

        if (existingId != null) {
            bookmarkState.remove(url)
            viewModelScope.launch {
                try { bookmarkRepository.deleteBookmark(existingId) }
                catch (_: Exception) { bookmarkState.add(url, existingId) }
            }
        } else {
            bookmarkState.add(url, -1)
            viewModelScope.launch {
                try {
                    val bm = bookmarkRepository.createBookmark(articleId)
                    bookmarkState.add(url, bm.id)
                } catch (_: Exception) { bookmarkState.remove(url) }
            }
        }
        return true
    }
}
