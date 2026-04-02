package com.boancurator.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boancurator.app.data.model.ApiLevel
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SortMode(val label: String) {
    RECENT("최신순"),
    RECOMMENDED("추천")
}

data class HomeUiState(
    val articles: List<CardView> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val selectedLevel: String? = null,
    val sortMode: SortMode = SortMode.RECENT,
    val hasMore: Boolean = true,
    val offset: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        loadArticles()
    }

    fun onSortModeSelected(mode: SortMode) {
        if (_uiState.value.sortMode == mode) return
        _uiState.value = _uiState.value.copy(
            sortMode = mode,
            articles = emptyList(),
            offset = 0,
            hasMore = true
        )
        loadArticles()
    }

    fun onLevelSelected(level: String?) {
        _uiState.value = _uiState.value.copy(
            selectedLevel = level,
            articles = emptyList(),
            offset = 0,
            hasMore = true
        )
        loadArticles()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore || state.sortMode == SortMode.RECOMMENDED) return
        _uiState.value = state.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val response = articleRepository.getCardNews(
                    level = state.selectedLevel,
                    offset = state.offset,
                    limit = PAGE_SIZE
                )
                _uiState.value = _uiState.value.copy(
                    articles = _uiState.value.articles + response.items,
                    offset = _uiState.value.offset + response.items.size,
                    hasMore = response.hasMore,
                    isLoadingMore = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false, error = e.message)
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(
            articles = emptyList(),
            offset = 0,
            hasMore = true
        )
        loadArticles()
    }

    private fun loadArticles() {
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                when (_uiState.value.sortMode) {
                    SortMode.RECENT -> {
                        val response = articleRepository.getCardNews(
                            level = _uiState.value.selectedLevel,
                            offset = 0,
                            limit = PAGE_SIZE
                        )
                        _uiState.value = _uiState.value.copy(
                            articles = response.items,
                            offset = response.items.size,
                            hasMore = response.hasMore,
                            isLoading = false,
                            error = null
                        )
                    }
                    SortMode.RECOMMENDED -> {
                        try {
                            val recs = articleRepository.getRecommendations()
                            _uiState.value = _uiState.value.copy(
                                articles = recs,
                                hasMore = false,
                                isLoading = false,
                                error = null
                            )
                        } catch (_: Exception) {
                            // 비로그인 시 추천 불가 → 최신순으로 fallback
                            val response = articleRepository.getCardNews(offset = 0, limit = PAGE_SIZE)
                            _uiState.value = _uiState.value.copy(
                                articles = response.items,
                                offset = response.items.size,
                                hasMore = response.hasMore,
                                isLoading = false,
                                sortMode = SortMode.RECENT,
                                error = "로그인 후 추천 기능을 이용할 수 있습니다"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun getLevels() = ApiLevel.all
    fun getLevelLabel(level: String) = ApiLevel.toKorean(level)
}
