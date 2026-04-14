package com.boancurator.app.ui.screens.article

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.boancurator.app.data.model.ApiCategory
import com.boancurator.app.data.model.ApiLevel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary
import com.boancurator.app.ui.theme.Warning

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ArticleDetailScreen(
    articleId: Int,
    url: String,
    onBack: () -> Unit,
    viewModel: ArticleDetailViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val bookmarkMap by viewModel.bookmarkState.bookmarkMap.collectAsStateWithLifecycle()
    val currentRating by viewModel.currentRating.collectAsStateWithLifecycle()
    val cardView by viewModel.cardView.collectAsStateWithLifecycle()
    val isBookmarked = url in bookmarkMap
    var pageTitle by remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var showSummarySheet by remember { mutableStateOf(false) }
    val summarySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.runtime.LaunchedEffect(articleId, url) {
        viewModel.loadRating(articleId)
        viewModel.loadCardView(url)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top bar
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkCard)
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "뒤로",
                        tint = Cyan
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = pageTitle.ifEmpty { "로딩 중..." },
                        color = TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = Uri.parse(url).host ?: url,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = {
                    if (articleId > 0) {
                        val toggled = viewModel.toggleBookmark(articleId, url)
                        if (!toggled) {
                            Toast.makeText(context, "로그인 후 북마크할 수 있습니다", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Icon(
                        if (isBookmarked) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                        contentDescription = "북마크",
                        tint = if (isBookmarked) Warning else TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 좋아요
                IconButton(onClick = {
                    if (!viewModel.rateArticle(articleId, 1)) {
                        Toast.makeText(context, "로그인 후 평가할 수 있습니다", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = "좋아요",
                        tint = if (currentRating == 1) Cyan else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 싫어요
                IconButton(onClick = {
                    if (!viewModel.rateArticle(articleId, -1)) {
                        Toast.makeText(context, "로그인 후 평가할 수 있습니다", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(
                        Icons.Filled.ThumbDown,
                        contentDescription = "싫어요",
                        tint = if (currentRating == -1) com.boancurator.app.ui.theme.Error else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // AI 한국어 요약
                IconButton(onClick = { showSummarySheet = true }) {
                    Icon(
                        Icons.Filled.Description,
                        contentDescription = "AI 요약",
                        tint = Cyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, url)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "공유"))
                }) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "공유",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }) {
                    Icon(
                        Icons.Filled.OpenInBrowser,
                        contentDescription = "브라우저에서 열기",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // Cyan accent line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Cyan.copy(alpha = 0.3f))
            )
        }

        // Progress bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = Cyan,
                trackColor = DarkCard
            )
        }

        // AI 요약 바텀시트
        if (showSummarySheet) {
            val card = cardView
            ModalBottomSheet(
                onDismissRequest = { showSummarySheet = false },
                sheetState = summarySheetState,
                containerColor = DarkCard,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "🤖 AI 요약",
                        color = Cyan,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = card?.title ?: pageTitle,
                        color = TextPrimary,
                        fontSize = 17.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row {
                        card?.category?.let { cat ->
                            Box(
                                modifier = Modifier
                                    .background(Cyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = ApiCategory.toKorean(cat),
                                    color = Cyan,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.size(6.dp))
                        }
                        card?.level?.let { lv ->
                            val (label, color) = when (lv) {
                                "Low" -> "초급" to com.boancurator.app.ui.theme.LevelBeginner
                                "Medium" -> "중급" to com.boancurator.app.ui.theme.LevelIntermediate
                                "High" -> "고급" to com.boancurator.app.ui.theme.LevelAdvanced
                                else -> ApiLevel.toKorean(lv) to TextSecondary
                            }
                            Box(
                                modifier = Modifier
                                    .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(text = label, color = color, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = card?.summary ?: "요약 정보가 없습니다.",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).also { webViewRef = it }.apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, u: String?, favicon: Bitmap?) {
                            isLoading = true
                        }

                        override fun onPageFinished(view: WebView?, u: String?) {
                            isLoading = false
                            pageTitle = view?.title ?: ""
                            viewModel.markAsRead(articleId)
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            Log.e("ArticleDetail", "WebView error: ${error?.errorCode} ${error?.description} url=${request?.url}")
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            return false // 모든 URL을 WebView 내에서 로드
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                        }
                    }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        allowFileAccess = false
                        allowContentAccess = false
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        // 일반 Chrome User-Agent 사용 (WebView 차단 우회)
                        userAgentString = userAgentString.replace("; wv", "")
                    }

                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
