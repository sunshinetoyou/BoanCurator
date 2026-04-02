package com.boancurator.app.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boancurator.app.data.model.ApiTheme
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<CardView> = emptyList(),
    val themes: List<String> = ApiTheme.all,
    val selectedTheme: String? = null,
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null,
    val searchMode: SearchMode = SearchMode.SEMANTIC
)

enum class SearchMode { SEMANTIC, THEME }

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val articleRepository: ArticleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)

        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(500)
                performSearch()
            }
        } else if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(results = emptyList(), hasSearched = false)
        }
    }

    fun onSearchModeChanged(mode: SearchMode) {
        _uiState.value = _uiState.value.copy(
            searchMode = mode,
            results = emptyList(),
            hasSearched = false
        )
    }

    fun onThemeSelected(theme: String) {
        _uiState.value = _uiState.value.copy(selectedTheme = theme, isLoading = true)
        viewModelScope.launch {
            try {
                val response = articleRepository.searchByTheme(listOf(theme))
                _uiState.value = _uiState.value.copy(
                    results = response.items,
                    isLoading = false,
                    hasSearched = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun search() {
        searchJob?.cancel()
        performSearch()
    }

    private fun performSearch() {
        val query = _uiState.value.query
        if (query.isBlank()) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                val results = articleRepository.searchSemantic(query)
                _uiState.value = _uiState.value.copy(
                    results = results,
                    isLoading = false,
                    hasSearched = true,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
}
