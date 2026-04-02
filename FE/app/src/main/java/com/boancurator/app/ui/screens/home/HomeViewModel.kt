package com.boancurator.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.boancurator.app.data.model.CardView
import com.boancurator.app.data.repository.ArticleRepository
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
    val availableSources: List<String> = emptyList()
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
        loadYears()
        loadArticles()
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
        for (week in _uiState.value.weekGroups) {
            if (week.year == year) return index
            index++
            if (!week.collapsed) {
                for (day in week.days) {
                    index++
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
                val newAll = _uiState.value.allArticles + response.items
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
                _uiState.value = _uiState.value.copy(
                    allArticles = response.items,
                    offset = response.items.size,
                    hasMore = response.hasMore,
                    isLoading = false,
                    error = null
                )
                updateSources()
                rebuildGroups()
                updateYearsFromData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
        // 최신 주차 + 최신 날짜 펼침
        val expanded = weeks.mapIndexed { i, week ->
            if (i == 0) {
                week.copy(
                    collapsed = false,
                    days = week.days.mapIndexed { j, day ->
                        if (j == 0) day.copy(collapsed = false) else day
                    }
                )
            } else week
        }
        _uiState.value = _uiState.value.copy(weekGroups = expanded)
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
