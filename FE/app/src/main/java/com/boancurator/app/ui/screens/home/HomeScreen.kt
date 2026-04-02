package com.boancurator.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boancurator.app.ui.components.ArticleCard
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCardBorder
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val CyanBright = Color(0xFF00D4FF)
private val CyanDimmed = Color(0xFF1A4A5C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Auto-load when data is insufficient
    LaunchedEffect(uiState.weekGroups, uiState.hasMore, uiState.isLoadingMore) {
        if (uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoading) {
            val total = uiState.allArticles.size
            if (total < 60) viewModel.loadMore()
        }
    }

    // Scroll-end load
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisible >= totalItems - 3 && !uiState.isLoadingMore && uiState.hasMore
        }
    }
    LaunchedEffect(shouldLoadMore) { if (shouldLoadMore) viewModel.loadMore() }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        // === Filter bar ===
        FilterBar(
            uiState = uiState,
            onFieldSelected = { viewModel.onFieldSelected(it) },
            onSourceSelected = { viewModel.onSourceSelected(it) },
            onToggleExpanded = { viewModel.toggleFiltersExpanded() }
        )

        // === Content ===
        val currentYear = uiState.years.firstOrNull() ?: java.time.LocalDate.now().year

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.error != null && uiState.weekGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "연결할 수 없습니다", color = TextPrimary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("당겨서 새로고침", color = TextMuted, fontSize = 12.sp)
                    }
                }
            } else if (!uiState.isLoading && uiState.weekGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("해당 조건의 기사가 없습니다", color = TextMuted, fontSize = 14.sp)
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        var lastYear: Int? = null

                        uiState.weekGroups.forEach { week ->
                            val isCurrent = week.year == currentYear
                            val yearColor = if (isCurrent) CyanBright else CyanDimmed

                            // Year divider
                            if (week.year != lastYear) {
                                item(key = "year_${week.year}") {
                                    YearDivider(year = week.year, isCurrent = isCurrent)
                                }
                                lastYear = week.year
                            }

                            // Week header
                            item(key = "week_${week.key}") {
                                WeekHeader(
                                    label = week.label, dateRange = week.dateRange,
                                    count = week.totalCount, collapsed = week.collapsed,
                                    accentColor = yearColor,
                                    onClick = { viewModel.toggleWeek(week.key) }
                                )
                            }

                            if (!week.collapsed) {
                                week.days.forEach { day ->
                                    item(key = "day_${day.date}") {
                                        DayHeader(
                                            date = day.date.format(DateTimeFormatter.ofPattern("M/d (E)")),
                                            count = day.articles.size,
                                            collapsed = day.collapsed,
                                            onClick = { viewModel.toggleDay(day.date) }
                                        )
                                    }
                                    if (!day.collapsed) {
                                        items(items = day.articles, key = { "a_${it.url}" }) { article ->
                                            ArticleCard(
                                                article = article,
                                                onClick = {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
                                                },
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.isLoadingMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Cyan, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                }
                            }
                        }
                    }

                    // Year scroll indicator
                    if (uiState.years.isNotEmpty()) {
                        YearScrollIndicator(
                            years = uiState.years, currentYear = currentYear,
                            onYearSelected = { year ->
                                scope.launch { listState.animateScrollToItem(viewModel.getWeekIndexForYear(year)) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterBar(
    uiState: HomeUiState,
    onFieldSelected: (SecurityField?) -> Unit,
    onSourceSelected: (String?) -> Unit,
    onToggleExpanded: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(top = 8.dp, bottom = 4.dp)
    ) {
        // 분야 필터 (항상 표시)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp)
        ) {
            FilterPill(
                text = "전체",
                selected = uiState.selectedField == null,
                onClick = { onFieldSelected(null) }
            )
            SecurityField.entries.forEach { field ->
                FilterPill(
                    text = field.label,
                    selected = uiState.selectedField == field,
                    onClick = { onFieldSelected(field) }
                )
            }
        }

        // 더보기 / 접기
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (uiState.filtersExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Cyan.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 출처 필터 (더보기 시 표시)
        AnimatedVisibility(
            visible = uiState.filtersExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    "출처",
                    color = Cyan.copy(alpha = 0.5f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 14.dp, bottom = 4.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp)
                ) {
                    FilterPill(
                        text = "전체",
                        selected = uiState.selectedSource == null,
                        onClick = { onSourceSelected(null) }
                    )
                    uiState.availableSources.forEach { source ->
                        FilterPill(
                            text = source,
                            selected = uiState.selectedSource == source,
                            onClick = { onSourceSelected(source) }
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
}

@Composable
private fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Cyan.copy(alpha = 0.12f) else DarkBackground)
            .clickable(onClick = onClick)
            .then(
                if (selected) Modifier else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) Cyan else TextMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun YearDivider(year: Int, isCurrent: Boolean) {
    val color = if (isCurrent) CyanBright else TextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) color.copy(alpha = 0.05f) else DarkSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(18.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(10.dp))
        Text("${year}년", color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun WeekHeader(
    label: String, dateRange: String, count: Int,
    collapsed: Boolean, accentColor: Color, onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(DarkSurface)
                .padding(start = 4.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(3.dp).height(30.dp).background(accentColor.copy(alpha = 0.5f)))
                Spacer(Modifier.width(10.dp))
                Icon(
                    if (collapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Column {
                    Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(dateRange, color = TextMuted, fontSize = 11.sp)
                }
            }
            Text("${count}건", color = TextSecondary, fontSize = 11.sp)
        }
        HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
    }
}

@Composable
private fun DayHeader(date: String, count: Int, collapsed: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (collapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(date, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text("${count}건", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun YearScrollIndicator(years: List<Int>, currentYear: Int, onYearSelected: (Int) -> Unit) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .width(34.dp)
            .fillMaxHeight()
            .background(DarkSurface.copy(alpha = 0.7f))
            .pointerInput(years) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val idx = ((change.position.y / size.height.toFloat()) * years.size).toInt().coerceIn(0, years.size - 1)
                    if (idx != selectedIndex) { selectedIndex = idx; onYearSelected(years[idx]) }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        years.forEachIndexed { index, year ->
            val isSel = index == selectedIndex
            val isCur = year == currentYear
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSel) Cyan.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { selectedIndex = index; onYearSelected(year) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${year % 100}",
                    color = if (isSel) CyanBright else if (isCur) CyanBright.copy(alpha = 0.6f) else TextMuted,
                    fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
