package com.boancurator.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.repository.ArticleRepository
import com.boancurator.app.data.repository.AuthRepository
import com.boancurator.app.data.repository.BookmarkRepository
import com.boancurator.app.data.repository.BookmarkStateHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
<<<<<<< Updated upstream
    val bookmarkMap: Map<String, Int> = emptyMap()
) {
    val bookmarkedUrls: Set<String> get() = bookmarkMap.keys
}
=======
    val hasMore: Boolean = true,
    val offset: Int = 0,
    // Filters
    val selectedField: SecurityField? = null,
    val selectedSource: String? = null,
    val filtersExpanded: Boolean = false,
    val bookmarkOnly: Boolean = false,
    val bookmarkArticles: List<CardView> = emptyList(),
    // Sources extracted from data
    val availableSources: List<String> = emptyList(),
)
>>>>>>> Stashed changes

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val authRepository: AuthRepository,
    val bookmarkState: BookmarkStateHolder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

<<<<<<< Updated upstream
    private var _isAuthenticated = false
=======
    private val _toastEvent = Channel<String>(Channel.BUFFERED)
    val toastEvent = _toastEvent.receiveAsFlow()

    private fun sendToast(message: String) {
        viewModelScope.launch { _toastEvent.send(message) }
    }
>>>>>>> Stashed changes

    companion object {
        private const val FEED_SIZE = 10
    }

    init {
<<<<<<< Updated upstream
        refreshAuthStatus()
        loadFeed()
        loadBookmarkState()
=======
        observeAuthState()
        loadFromCacheThenSync()
    }

    /** 토큰 변경을 실시간 감지 — 로그인/로그아웃 자동 반영 */
    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.isLoggedIn.collect { loggedIn ->
                if (loggedIn) {
                    loadBookmarkState()
                } else {
                    bookmarkState.clear()
                }
            }
        }
>>>>>>> Stashed changes
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
                val bookmarks = bookmarkRepository.getBookmarks()
                val map = bookmarks.associate { it.url to it.bookmarkId }
                bookmarkState.update(map)
            } catch (_: Exception) {}
        }
    }

<<<<<<< Updated upstream
    private fun refreshAuthStatus() {
        viewModelScope.launch {
            _isAuthenticated = authRepository.isAuthenticated()
        }
    }
=======
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
>>>>>>> Stashed changes

    /** @return true if bookmark toggled, false if not logged in */
    fun toggleBookmark(article: CardView): Boolean {
        if (!authRepository.isLoggedIn.value) return false
        val articleId = article.articleId ?: return false
        val url = article.url ?: return false

        val existingBookmarkId = bookmarkState.getBookmarkId(url)

        if (existingBookmarkId != null) {
<<<<<<< Updated upstream
            _uiState.value = _uiState.value.copy(bookmarkMap = currentMap - article.url)
=======
            bookmarkState.remove(url)
>>>>>>> Stashed changes
            viewModelScope.launch {
                try {
                    bookmarkRepository.deleteBookmark(existingBookmarkId)
                } catch (e: Exception) {
                    Log.e("HomeVM", "Bookmark delete failed", e)
<<<<<<< Updated upstream
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
=======
                    bookmarkState.add(url, existingBookmarkId)
                }
            }
        } else {
            bookmarkState.add(url, -1)
            viewModelScope.launch {
                try {
                    val bookmark = bookmarkRepository.createBookmark(articleId)
                    bookmarkState.add(url, bookmark.id)
>>>>>>> Stashed changes
                } catch (e: Exception) {
                    Log.e("HomeVM", "Bookmark create failed", e)
                    bookmarkState.remove(url)
                }
            }
        }
        return true
    }
<<<<<<< Updated upstream
=======

    // === Bookmark filter ===
    fun onBookmarkFilterToggled() {
        val newVal = !_uiState.value.bookmarkOnly
        _uiState.value = _uiState.value.copy(bookmarkOnly = newVal)
        if (newVal) {
            loadBookmarkArticles()
        }
    }

    private fun loadBookmarkArticles() {
        viewModelScope.launch {
            try {
                val cards = bookmarkRepository.getBookmarkedCards()
                _uiState.value = _uiState.value.copy(bookmarkArticles = cards)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(bookmarkArticles = emptyList())
            }
        }
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
                val msg = if (e is java.net.UnknownHostException) "인터넷 연결을 확인해주세요"
                    else "추가 기사를 불러오지 못했습니다"
                sendToast(msg)
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun refresh() {
        sendToast("최신 데이터를 불러오는 중...")
        viewModelScope.launch {
            try { articleRepository.clearCache() } catch (_: Exception) {}
            _uiState.value = _uiState.value.copy(
                allArticles = emptyList(),
                weekGroups = emptyList(),
                availableSources = emptyList(),
                offset = 0,
                hasMore = true,
                error = null
            )
            loadYears()
            loadArticles()
            loadBookmarkState()
        }
    }

    private fun loadArticles() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                // API에서 최신 데이터
                val response = articleRepository.getCardNews(offset = 0, limit = PAGE_SIZE)

                // Room 캐시에서 전체 데이터 읽기
                val cached = try { articleRepository.getCachedArticles() } catch (_: Exception) { emptyList() }

                // 병합: API 최신 + 캐시 (중복 제거)
                val apiUrls = response.items.map { it.url }.toSet()
                val merged = response.items + cached.filter { it.url !in apiUrls }

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
                val msg = if (e is java.net.UnknownHostException) "인터넷 연결을 확인해주세요"
                    else "기사를 불러오지 못했습니다"
                sendToast(msg)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.allArticles.isEmpty()) msg else null
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
        val sources = _uiState.value.allArticles.mapNotNull { it.source }.distinct().sorted()
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
>>>>>>> Stashed changes
}
