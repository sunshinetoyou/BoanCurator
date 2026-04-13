package com.boancurator.app.ui.screens.home

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
        )

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            when {
                uiState.error != null && uiState.articles.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error ?: "연결할 수 없습니다", color = TextPrimary, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("당겨서 새로고침", color = TextMuted, fontSize = 12.sp)
                        }
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
                        items(items = uiState.articles, key = { it.url }) { article ->
                            ArticleCard(
                                article = article,
                                onClick = { onArticleClick(article) },
                                isBookmarked = article.url in uiState.bookmarkedUrls,
                                onBookmarkClick = { onBookmarkClick(article) },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
