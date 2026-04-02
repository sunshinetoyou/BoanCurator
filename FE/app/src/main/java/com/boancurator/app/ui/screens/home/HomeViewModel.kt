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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

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

data class HomeUiState(
    val years: List<Int> = emptyList(),
    val weekGroups: List<WeekGroup> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
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
        loadYears()
        loadArticles()
    }

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

    private fun updateYearsFromData() {
        val years = _uiState.value.weekGroups.map { it.year }.distinct().sortedDescending()
        if (years.isNotEmpty() && _uiState.value.years.isEmpty()) {
            _uiState.value = _uiState.value.copy(years = years)
        }
    }

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

    fun loadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasMore) return
        _uiState.value = state.copy(isLoadingMore = true)

        viewModelScope.launch {
            try {
                val response = articleRepository.getCardNews(offset = state.offset, limit = PAGE_SIZE)
                val newWeeks = buildWeekGroups(response.items)
                val merged = mergeWeekGroups(_uiState.value.weekGroups, newWeeks)
                _uiState.value = _uiState.value.copy(
                    weekGroups = merged,
                    offset = _uiState.value.offset + response.items.size,
                    hasMore = response.hasMore,
                    isLoadingMore = false,
                    error = null
                )
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
                val weeks = buildWeekGroups(response.items)
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
                _uiState.value = _uiState.value.copy(
                    weekGroups = expanded,
                    offset = response.items.size,
                    hasMore = response.hasMore,
                    isLoading = false,
                    error = null
                )
                updateYearsFromData()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
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
                // 월 기준 주차: 해당 월의 몇번째 주인지 계산
                val date = day.date
                val year = date.year
                val month = date.monthValue
                val weekOfMonth = (date.dayOfMonth - 1) / 7 + 1
                Triple(year, month, weekOfMonth)
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

    private fun mergeWeekGroups(existing: List<WeekGroup>, new: List<WeekGroup>): List<WeekGroup> {
        val map = existing.associateBy { it.key }.toMutableMap()
        for (week in new) {
            val prev = map[week.key]
            if (prev != null) {
                val dayMap = prev.days.associateBy { it.date }.toMutableMap()
                for (day in week.days) {
                    val prevDay = dayMap[day.date]
                    if (prevDay != null) {
                        val merged = prevDay.articles + day.articles.filter { a -> prevDay.articles.none { it.url == a.url } }
                        dayMap[day.date] = prevDay.copy(articles = merged)
                    } else {
                        dayMap[day.date] = day
                    }
                }
                // 날짜 범위 업데이트
                val allDays = dayMap.values.sortedByDescending { it.date }
                val dayFmt = DateTimeFormatter.ofPattern("M/d")
                val newRange = "${allDays.last().date.format(dayFmt)} ~ ${allDays.first().date.format(dayFmt)}"
                map[week.key] = prev.copy(days = allDays.toList(), dateRange = newRange)
            } else {
                map[week.key] = week
            }
        }
        return map.values.sortedWith(
            compareByDescending<WeekGroup> { it.year }
                .thenByDescending { it.month }
                .thenByDescending { it.weekOfMonth }
        )
    }
}

fun getSecurityField(themes: List<String>?): String {
    if (themes.isNullOrEmpty()) return "ETC"
    if ("Security" !in themes) return "ETC"
    return when {
        "AI/ML" in themes -> "AI 보안"
        "Infra/Cloud" in themes -> "인프라 보안"
        "Development" in themes -> "개발 보안"
        "Business/Policy" in themes -> "보안 정책"
        else -> "보안 일반"
    }
}
