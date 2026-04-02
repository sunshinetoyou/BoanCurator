package com.boancurator.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.DarkCardBorder
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.NeonGreen
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

// 현재 연도 = 네온 그린, 다른 연도 = 탁한 네온 그린
private val NeonGreenBright = Color(0xFF00FF41)
private val NeonGreenDimmed = Color(0xFF1A5C2A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 접힌 상태에서도 데이터 부족하면 자동 로드
    LaunchedEffect(uiState.weekGroups, uiState.hasMore, uiState.isLoadingMore) {
        if (uiState.hasMore && !uiState.isLoadingMore && !uiState.isLoading) {
            val totalArticles = uiState.weekGroups.sumOf { it.totalCount }
            if (totalArticles < 50) {
                viewModel.loadMore()
            }
        }
    }

    // 스크롤 끝 감지
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            totalItems > 0 && lastVisibleItem >= totalItems - 3 && !uiState.isLoadingMore && uiState.hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (uiState.error != null && uiState.weekGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(uiState.error ?: "연결할 수 없습니다", color = TextPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("당겨서 새로고침", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else {
            val currentYear = uiState.years.firstOrNull() ?: java.time.LocalDate.now().year

            Row(modifier = Modifier.fillMaxSize()) {
                // Main content
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    var lastYear: Int? = null

                    uiState.weekGroups.forEach { week ->
                        val isCurrent = week.year == currentYear
                        val yearColor = if (isCurrent) NeonGreenBright else NeonGreenDimmed

                        // 연도 구분 헤더
                        if (week.year != lastYear) {
                            item(key = "year_${week.year}") {
                                YearDivider(year = week.year, isCurrent = isCurrent)
                            }
                            lastYear = week.year
                        }

                        // Week header
                        item(key = "week_${week.key}") {
                            WeekHeader(
                                label = week.label,
                                dateRange = week.dateRange,
                                count = week.totalCount,
                                collapsed = week.collapsed,
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
                                    items(
                                        items = day.articles,
                                        key = { "a_${it.url}" }
                                    ) { article ->
                                        ArticleCard(
                                            article = article,
                                            onClick = {
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = NeonGreen, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }

                // Year scroll indicator
                if (uiState.years.isNotEmpty()) {
                    YearScrollIndicator(
                        years = uiState.years,
                        currentYear = currentYear,
                        onYearSelected = { year ->
                            val index = viewModel.getWeekIndexForYear(year)
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun YearDivider(year: Int, isCurrent: Boolean) {
    val color = if (isCurrent) NeonGreenBright else NeonGreenDimmed
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isCurrent) color.copy(alpha = 0.06f) else DarkSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "${year}년",
            color = if (isCurrent) NeonGreenBright else TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun WeekHeader(
    label: String,
    dateRange: String,
    count: Int,
    collapsed: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(DarkSurface)
                .padding(start = 4.dp, end = 16.dp, top = 11.dp, bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 왼쪽 색상 바
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(32.dp)
                        .background(accentColor.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = if (collapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(dateRange, color = TextMuted, fontSize = 11.sp)
                }
            }
            Text("${count}건", color = TextSecondary, fontSize = 12.sp)
        }
        HorizontalDivider(color = DarkCardBorder, thickness = 0.5.dp)
    }
}

@Composable
private fun DayHeader(
    date: String,
    count: Int,
    collapsed: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(DarkBackground)
            .padding(horizontal = 32.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (collapsed) Icons.Filled.KeyboardArrowRight else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(date, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Text("${count}건", color = TextMuted, fontSize = 11.sp)
    }
}

@Composable
private fun YearScrollIndicator(
    years: List<Int>,
    currentYear: Int,
    onYearSelected: (Int) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .width(36.dp)
            .fillMaxHeight()
            .background(DarkSurface.copy(alpha = 0.7f))
            .pointerInput(years) {
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val y = change.position.y
                    val height = size.height.toFloat()
                    val index = ((y / height) * years.size).toInt().coerceIn(0, years.size - 1)
                    if (index != selectedIndex) {
                        selectedIndex = index
                        onYearSelected(years[index])
                    }
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        years.forEachIndexed { index, year ->
            val isSelected = index == selectedIndex
            val isCurrent = year == currentYear
            val color = if (isCurrent) NeonGreenBright else NeonGreenDimmed
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable {
                        selectedIndex = index
                        onYearSelected(year)
                    }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${year % 100}",
                    color = if (isSelected) NeonGreenBright else if (isCurrent) NeonGreenBright.copy(alpha = 0.7f) else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
