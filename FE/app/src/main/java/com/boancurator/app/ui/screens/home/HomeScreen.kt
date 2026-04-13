package com.boancurator.app.ui.screens.home

<<<<<<< Updated upstream
import android.content.Intent
import android.net.Uri
import android.widget.Toast
=======
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
>>>>>>> Stashed changes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boancurator.app.data.model.CardView
import com.boancurator.app.ui.components.ArticleCard
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
<<<<<<< Updated upstream
=======
import com.boancurator.app.ui.theme.TextSecondary
import androidx.navigation.NavController
import com.boancurator.app.navigation.Screen
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
>>>>>>> Stashed changes

@Composable
fun HomeRoute(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    HomeScreen(
        uiState = uiState,
        onRefresh = viewModel::refresh,
        onArticleClick = { article ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
        },
        onBookmarkClick = { article ->
            val toggled = viewModel.toggleBookmark(article)
            if (!toggled) {
                Toast.makeText(context, "로그인 후 북마크할 수 있습니다", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
<<<<<<< Updated upstream
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onArticleClick: (CardView) -> Unit,
    onBookmarkClick: (CardView) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        Text(
            text = "최신 피드",
            color = Cyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
=======
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarkMap by viewModel.bookmarkState.bookmarkMap.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Toast 이벤트 수집
    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 북마크 상태는 BookmarkStateHolder로 공유 — 탭 전환 시 즉시 반영

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
            onToggleExpanded = { viewModel.toggleFiltersExpanded() },
            onBookmarkToggled = { viewModel.onBookmarkFilterToggled() }
>>>>>>> Stashed changes
        )

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
<<<<<<< Updated upstream
            when {
                uiState.error != null && uiState.articles.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error ?: "연결할 수 없습니다", color = TextPrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("당겨서 새로고침", color = TextMuted, fontSize = 12.sp)
                        }
=======
            if (uiState.bookmarkOnly) {
                // === 북마크 모드: flat list ===
                if (uiState.bookmarkArticles.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("북마크한 기사가 없습니다", color = TextMuted, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(items = uiState.bookmarkArticles, key = { "bm_${it.url}" }) { article ->
                            ArticleCard(
                                article = article,
                                onClick = {
                                    article.url?.let {
                                                        navController.navigate(Screen.ArticleDetail.createRoute(article.articleId, it))
                                                    }
                                },
                                isBookmarked = true,
                                onBookmarkClick = {
                                    viewModel.toggleBookmark(article)
                                    // 북마크 해제 후 목록 갱신
                                    viewModel.onBookmarkFilterToggled()
                                    viewModel.onBookmarkFilterToggled()
                                }
                            )
                        }
                    }
                }
            } else if (uiState.error != null && uiState.weekGroups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error ?: "연결할 수 없습니다", color = TextPrimary, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("당겨서 새로고침", color = TextMuted, fontSize = 12.sp)
>>>>>>> Stashed changes
                    }
                }
                !uiState.isLoading && uiState.articles.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("새로운 기사가 없습니다", color = TextMuted, fontSize = 14.sp)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
<<<<<<< Updated upstream
                        items(items = uiState.articles, key = { it.url }) { article ->
                            ArticleCard(
                                article = article,
                                onClick = { onArticleClick(article) },
                                isBookmarked = article.url in uiState.bookmarkedUrls,
                                onBookmarkClick = { onBookmarkClick(article) },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
=======
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
                                                    article.url?.let {
                                                        navController.navigate(Screen.ArticleDetail.createRoute(article.articleId, it))
                                                    }
                                                },
                                                isBookmarked = article.url in bookmarkMap,
                                                onBookmarkClick = {
                                                    val toggled = viewModel.toggleBookmark(article)
                                                    if (!toggled) {
                                                        Toast.makeText(context, "로그인 후 북마크할 수 있습니다", Toast.LENGTH_SHORT).show()
                                                    }
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
>>>>>>> Stashed changes
                        }
                    }
                }
            }
        }
    }
}
<<<<<<< Updated upstream
=======

@Composable
private fun FilterBar(
    uiState: HomeUiState,
    onFieldSelected: (SecurityField?) -> Unit,
    onSourceSelected: (String?) -> Unit,
    onToggleExpanded: () -> Unit,
    onBookmarkToggled: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
    ) {
        // 북마크 토글 + 활성 필터 요약
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            FilterPill(
                text = "전체",
                selected = !uiState.bookmarkOnly,
                onClick = { if (uiState.bookmarkOnly) onBookmarkToggled() }
            )
            FilterPill(
                text = "북마크",
                selected = uiState.bookmarkOnly,
                onClick = { if (!uiState.bookmarkOnly) onBookmarkToggled() }
            )
        }

        // 활성 필터 요약 + 더보기 토글 (북마크 모드가 아닐 때만)
        if (!uiState.bookmarkOnly) Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (uiState.filtersExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Cyan.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                // 현재 적용된 필터 표시
                val activeFilters = mutableListOf<String>()
                if (uiState.selectedField != null) activeFilters.add(uiState.selectedField.label)
                if (uiState.selectedSource != null) activeFilters.add(uiState.selectedSource)

                if (activeFilters.isEmpty()) {
                    Text("필터", color = TextMuted, fontSize = 13.sp)
                } else {
                    Text(
                        activeFilters.joinToString(" · "),
                        color = Cyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 더보기: 분야 + 출처 (북마크 모드 아닐 때만)
        AnimatedVisibility(
            visible = uiState.filtersExpanded && !uiState.bookmarkOnly,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(bottom = 8.dp)) {
                // 분야
                Text(
                    "분야",
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
                    FilterPill("전체", uiState.selectedField == null) { onFieldSelected(null) }
                    SecurityField.entries.forEach { field ->
                        FilterPill(field.label, uiState.selectedField == field) { onFieldSelected(field) }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 출처
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
                    FilterPill("전체", uiState.selectedSource == null) { onSourceSelected(null) }
                    uiState.availableSources.forEach { source ->
                        FilterPill(source, uiState.selectedSource == source) { onSourceSelected(source) }
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
>>>>>>> Stashed changes
