package com.boancurator.app.ui.screens.search

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boancurator.app.data.model.CardView
import com.boancurator.app.ui.components.ArticleCard
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary

@Composable
fun SearchRoute(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SearchScreen(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onSearch = viewModel::search,
        onSearchModeChanged = viewModel::onSearchModeChanged,
        onThemeSelected = viewModel::onThemeSelected,
        onArticleClick = { article ->
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url)))
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onSearchModeChanged: (SearchMode) -> Unit,
    onThemeSelected: (String) -> Unit,
    onArticleClick: (CardView) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Text(
            text = "검색",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        val tabs = listOf("AI 검색", "테마 검색")
        val selectedTab = if (uiState.searchMode == SearchMode.SEMANTIC) 0 else 1

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkBackground,
            contentColor = Cyan,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Cyan
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        onSearchModeChanged(
                            if (index == 0) SearchMode.SEMANTIC else SearchMode.THEME
                        )
                    },
                    text = { Text(title, color = if (selectedTab == index) Cyan else TextMuted) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (uiState.searchMode) {
            SearchMode.SEMANTIC -> {
                TextField(
                    value = uiState.query,
                    onValueChange = onQueryChanged,
                    placeholder = { Text("보안 뉴스를 검색하세요...", color = TextMuted) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Cyan,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                SearchResults(uiState = uiState, onArticleClick = onArticleClick)
            }

            SearchMode.THEME -> {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    uiState.themes.forEach { theme ->
                        val isSelected = uiState.selectedTheme == theme
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Cyan.copy(alpha = 0.15f) else DarkCard)
                                .clickable { onThemeSelected(theme) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = theme,
                                color = if (isSelected) Cyan else TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                SearchResults(uiState = uiState, onArticleClick = onArticleClick)
            }
        }
    }
}

@Composable
private fun SearchResults(
    uiState: SearchUiState,
    onArticleClick: (CardView) -> Unit,
) {
    when {
        uiState.isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan, modifier = Modifier.size(32.dp))
            }
        }

        uiState.hasSearched && uiState.results.isEmpty() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("검색 결과가 없습니다", color = TextPrimary, fontSize = 16.sp)
                    Text("다른 키워드로 검색해보세요", color = TextMuted, fontSize = 14.sp)
                }
            }
        }

        uiState.results.isNotEmpty() -> {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.results, key = { it.url }) { article ->
                    ArticleCard(
                        article = article,
                        onClick = { onArticleClick(article) }
                    )
                }
            }
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (uiState.searchMode == SearchMode.SEMANTIC)
                        "AI 기반 시맨틱 검색으로\n관련 보안 뉴스를 찾아보세요"
                    else
                        "테마를 선택하면\n관련 기사를 볼 수 있습니다",
                    color = TextMuted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp
                )
            }
        }
    }
}
