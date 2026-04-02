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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// === 보안 분야 매핑 ===
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

// === 계층 구조 ===
data class DayGroup(
    val date: LocalDate,
    val articles: List<CardView>,
    val collapsed: Boolean = true
)

data class WeekGroup(
    val year: Int,
    val month: Int,
    val weekOfMonth: Int,
    val label: String,
    val dateRange: String,
    val days: List<DayGroup>,
    val collapsed: Boolean = true
) {
    val totalCount: Int get() = days.sumOf { it.articles.size }
    val key: String get() = "${year}_${month}_${weekOfMonth}"
}

// === UI State ===
data class HomeUiState(
    val years: List<Int> = emptyList(),
    val weekGroups: List<WeekGroup> = emptyList(),
    val allArticles: List<CardView> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val offset: Int = 0,
    // Filters
    val selectedField: SecurityField? = null,
    val selectedSource: String? = null,
    val filtersExpanded: Boolean = false,
    // Sources extracted from data
    val availableSources: List<String> = emptyList(),
    // Bookmarks
    val bookmarkedUrls: Set<String> = emptySet()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    companion object {
        private const val PAGE_SIZE = 20
    }

    init {
        loadFromCacheThenSync()
    }

    /** 캐시 먼저 표시 -> API로 최신 데이터 동기화 */
    private fun loadFromCacheThenSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // 1. 캐시에서 즉시 로드
            try {
                val cached = articleRepository.getCachedArticles()
                if (cached.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        allArticles = cached,
                        isLoading = false
                    )
                    rebuildGroups()
                    updateYearsFromData()
                }
            } catch (_: Exception) {}

            // 2. API에서 최신 데이터 동기화
            loadYears()
            loadArticles()

            // 3. 오래된 캐시 정리
            try { articleRepository.cleanOldCache() } catch (_: Exception) {}
        }
    }

    // === Bookmark ===
    /** @return true if bookmark toggled, false if not logged in */
    fun toggleBookmark(article: CardView): Boolean {
        val hasToken = kotlinx.coroutines.runBlocking { authRepository.isAuthenticated() }
        if (!hasToken) return false
        val articleId = article.articleId ?: return false

        val current = _uiState.value.bookmarkedUrls
        if (article.url in current) {
            _uiState.value = _uiState.value.copy(bookmarkedUrls = current - article.url)
            // API 삭제는 bookmarkId가 필요하므로 일단 UI만 토글
        } else {
            _uiState.value = _uiState.value.copy(bookmarkedUrls = current + article.url)
            viewModelScope.launch {
                try {
                    bookmarkRepository.createBookmark(articleId)
                } catch (e: Exception) {
                    Log.e("HomeVM", "Bookmark create failed", e)
                    // 실패 시 롤백
                    _uiState.value = _uiState.value.copy(
                        bookmarkedUrls = _uiState.value.bookmarkedUrls - article.url
                    )
                }
            }
        }
        return true
    }

    // === Filter actions ===
    fun toggleFiltersExpanded() {
        _uiState.value = _uiState.value.copy(filtersExpanded = !_uiState.value.filtersExpanded)
    }

    fun onFieldSelected(field: SecurityField?) {
        _uiState.value = _uiState.value.copy(selectedField = field)
        rebuildGroups()
    }

    fun onSourceSelected(source: String?) {
        _uiState.value = _uiState.value.copy(selectedSource = source)
        rebuildGroups()
    }

    // === Toggle actions ===
    fun toggleWeek(weekKey: String) {
        _uiState.value = _uiState.value.copy(
            weekGroups = _uiState.value.weekGroups.map { week ->
                if (week.key == weekKey) week.copy(collapsed = !week.collapsed) else week
            }
        )
    }

    fun toggleDay(date: LocalDate) {
        _uiState.value = _uiState.value.copy(
            weekGroups = _uiState.value.weekGroups.map { week ->
                week.copy(days = week.days.map { day ->
                    if (day.date == date) day.copy(collapsed = !day.collapsed) else day
                })
            }
        )
    }

    fun getWeekIndexForYear(year: Int): Int {
        var index = 0
        var lastYear: Int? = null
        for (week in _uiState.value.weekGroups) {
            // 연도 구분 헤더
            if (week.year != lastYear) {
                if (week.year == year) return index
                index++ // year divider item
                lastYear = week.year
            }
            index++ // week header item
            if (!week.collapsed) {
                for (day in week.days) {
                    index++ // day header
                    if (!day.collapsed) index += day.articles.size
                }
            }
        }
        return 0
    }

    // === Data loading ===
    private fun loadYears() {
        viewModelScope.launch {
            try {
                val years = articleRepository.getAvailableYears()
                if (years.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(years = years)
                }
            } catch (_: Exception) {}
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.value = state.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val response = articleRepository.getCardNews(offset = state.offset, limit = PAGE_SIZE)
                val existingUrls = _uiState.value.allArticles.map { it.url }.toSet()
                val newAll = _uiState.value.allArticles + response.items.filter { it.url !in existingUrls }
                _uiState.value = _uiState.value.copy(
                    allArticles = newAll,
                    offset = _uiState.value.offset + response.items.size,
                    hasMore = response.hasMore,
                    isLoadingMore = false,
                    error = null
                )
                updateSources()
                rebuildGroups()
                updateYearsFromData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false, error = e.message)
            }
        }
    }

    fun refresh() {
        _uiState.value = HomeUiState()
        loadYears()
        loadArticles()
    }

    private fun loadArticles() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val response = articleRepository.getCardNews(offset = 0, limit = PAGE_SIZE)
                // 기존 데이터와 병합 (URL 기준 중복 제거)
                val existing = _uiState.value.allArticles
                val newUrls = response.items.map { it.url }.toSet()
                val merged = response.items + existing.filter { it.url !in newUrls }
                val isFirstLoad = _uiState.value.weekGroups.isEmpty()

                _uiState.value = _uiState.value.copy(
                    allArticles = merged,
                    weekGroups = if (isFirstLoad) emptyList() else _uiState.value.weekGroups,
                    offset = response.items.size,
                    hasMore = response.hasMore,
                    isLoading = false,
                    error = null
                )
                updateSources()
                rebuildGroups()
                updateYearsFromData()
            } catch (e: Exception) {
                // API 실패해도 캐시 데이터가 있으면 표시 유지
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.allArticles.isEmpty()) e.message else null
                )
            }
        }
    }

    // === Filtering + sorting + grouping ===
    private fun rebuildGroups() {
        val state = _uiState.value
        var filtered = state.allArticles

        // Field filter
        if (state.selectedField != null) {
            filtered = filtered.filter { getSecurityField(it.themes) == state.selectedField }
        }

        // Source filter
        if (state.selectedSource != null) {
            filtered = filtered.filter { it.source == state.selectedSource }
        }

        // Sort: HIGH → MID → LOW, ETC last
        val sorted = filtered.sortedWith(
            compareBy<CardView> { if (getSecurityField(it.themes) == SecurityField.ETC) 1 else 0 }
                .thenBy { levelOrder(it.level) }
                .thenByDescending { it.publishedAt ?: "" }
        )

        val weeks = buildWeekGroups(sorted)

        // 기존 토글 상태 보존
        val prevWeekState = state.weekGroups.associate { it.key to it.collapsed }
        val prevDayState = state.weekGroups.flatMap { it.days }.associate { it.date to it.collapsed }
        val isFirstLoad = state.weekGroups.isEmpty()

        val restored = weeks.mapIndexed { i, week ->
            val weekCollapsed = prevWeekState[week.key] ?: (if (isFirstLoad && i == 0) false else true)
            week.copy(
                collapsed = weekCollapsed,
                days = week.days.mapIndexed { j, day ->
                    val dayCollapsed = prevDayState[day.date] ?: (if (isFirstLoad && i == 0 && j == 0) false else true)
                    day.copy(collapsed = dayCollapsed)
                }
            )
        }
        _uiState.value = _uiState.value.copy(weekGroups = restored)
    }

    private fun levelOrder(level: String?): Int = when (level) {
        "High" -> 0
        "Medium" -> 1
        "Low" -> 2
        else -> 3
    }

    private fun updateSources() {
        val sources = _uiState.value.allArticles.map { it.source }.distinct().sorted()
        _uiState.value = _uiState.value.copy(availableSources = sources)
    }

    private fun updateYearsFromData() {
        if (_uiState.value.years.isEmpty()) {
            val years = _uiState.value.weekGroups.map { it.year }.distinct().sortedDescending()
            if (years.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(years = years)
            }
        }
    }

    private fun buildWeekGroups(articles: List<CardView>): List<WeekGroup> {
        val dayFormatter = DateTimeFormatter.ofPattern("M/d")

        return articles
            .mapNotNull { article ->
                val date = article.publishedAt?.take(10)?.let {
                    try { LocalDate.parse(it) } catch (_: Exception) { null }
                }
                date?.let { it to article }
            }
            .groupBy { (date, _) -> date }
            .map { (date, pairs) -> DayGroup(date = date, articles = pairs.map { it.second }) }
            .sortedByDescending { it.date }
            .groupBy { day ->
                val date = day.date
                Triple(date.year, date.monthValue, (date.dayOfMonth - 1) / 7 + 1)
            }
            .map { (key, days) ->
                val (year, month, weekOfMonth) = key
                val sortedDays = days.sortedByDescending { it.date }
                val firstDate = sortedDays.last().date
                val lastDate = sortedDays.first().date

                WeekGroup(
                    year = year,
                    month = month,
                    weekOfMonth = weekOfMonth,
                    label = "${month}월 ${weekOfMonth}주차",
                    dateRange = "${firstDate.format(dayFormatter)} ~ ${lastDate.format(dayFormatter)}",
                    days = sortedDays
                )
            }
            .sortedWith(
                compareByDescending<WeekGroup> { it.year }
                    .thenByDescending { it.month }
                    .thenByDescending { it.weekOfMonth }
            )
    }

    fun getFields() = SecurityField.entries.toList()
}
