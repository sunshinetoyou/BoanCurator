package com.boancurator.app.ui.screens.feed

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boancurator.app.BuildConfig
import com.boancurator.app.ui.components.ArticleCard
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary
import androidx.navigation.NavController
import com.boancurator.app.navigation.Screen
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarkMap by viewModel.bookmarkState.bookmarkMap.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Cyan, modifier = Modifier.size(32.dp))
                }
            }

            !uiState.isLoggedIn -> {
                // 미로그인 안내
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Cyan,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "맞춤 추천 피드",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "로그인하면 관심 분야에 맞는\n보안 뉴스를 추천받을 수 있습니다",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val cm = CredentialManager.create(context)
                                        val opt = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build()
                                        val req = GetCredentialRequest.Builder().addCredentialOption(opt).build()
                                        val result = cm.getCredential(context, req)
                                        val token = GoogleIdTokenCredential.createFrom(result.credential.data)
                                        viewModel.loginWithGoogle(token.idToken)
                                    } catch (e: Exception) {
                                        Log.e("FeedScreen", "Google Sign-In failed", e)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Google로 로그인", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            uiState.articles.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Cyan, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("추천 기사가 아직 없습니다", color = TextPrimary, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("기사를 북마크하면 맞춤 추천이 시작됩니다", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }

            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Text(
                            "맞춤 추천",
                            color = Cyan,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(items = uiState.articles, key = { it.url ?: it.hashCode().toString() }) { article ->
                        ArticleCard(
                            article = article,
                            onClick = {
                                article.url?.let {
                                    navController.navigate(Screen.ArticleDetail.createRoute(article.articleId, it))
                                }
                            },
                            isBookmarked = (article.url ?: "") in bookmarkMap,
                            onBookmarkClick = {
                                viewModel.toggleBookmark(article)
                            }
                        )
                    }
                }
            }
        }
    }
}
