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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val isBookmarked = url in bookmarkMap
    var pageTitle by remember { mutableStateOf("") }
    var progress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    // Load rating for this article
    androidx.compose.runtime.LaunchedEffect(articleId) {
        viewModel.loadRating(articleId)
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

        // WebView
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
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
