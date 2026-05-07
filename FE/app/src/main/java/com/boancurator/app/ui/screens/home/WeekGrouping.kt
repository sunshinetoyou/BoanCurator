package com.boancurator.app.ui.screens.home

import com.boancurator.app.data.model.CardView
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    val key: String get() = "${year}_${month}_${weekOfMonth}"
    val totalCount: Int get() = days.sumOf { it.articles.size }
}

private val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")

internal fun buildWeekGroups(articles: List<CardView>): List<WeekGroup> {
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
                dateRange = "${firstDate.format(DAY_FORMATTER)} ~ ${lastDate.format(DAY_FORMATTER)}",
                days = sortedDays
            )
        }
        .sortedWith(
            compareByDescending<WeekGroup> { it.year }
                .thenByDescending { it.month }
                .thenByDescending { it.weekOfMonth }
        )
}
