package com.boancurator.app.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boancurator.app.BuildConfig
import com.boancurator.app.data.model.CardView
import com.boancurator.app.ui.components.LevelBadge
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    ProfileScreen(
        uiState = uiState,
        onGoogleSignIn = {
            scope.launch {
                try {
                    val credentialManager = CredentialManager.create(context)
                    val signInOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(signInOption)
                        .build()

                    val result = credentialManager.getCredential(context, request)
                    val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
                    viewModel.loginWithGoogle(googleIdToken.idToken)
                } catch (e: Exception) {
                    Log.e("ProfileScreen", "Google Sign-In failed", e)
                }
            }
        },
        onLogout = viewModel::logout
    )
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onGoogleSignIn: () -> Unit,
    onLogout: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "프로필",
                    color = TextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                if (uiState.isLoggedIn) {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "로그아웃",
                            tint = TextMuted
                        )
                    }
                }
            }
        }

        item {
            if (uiState.isLoggedIn && uiState.user != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.user.picture != null) {
                            AsyncImage(
                                model = uiState.user.picture,
                                contentDescription = "프로필 사진",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, Cyan.copy(alpha = 0.3f), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface)
                                    .border(2.dp, Cyan.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = uiState.user.name ?: "사용자",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.user.email,
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Shield,
                            contentDescription = null,
                            tint = Cyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "보안큐레이터에 로그인",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "북마크, 개인화 추천 등\n더 많은 기능을 이용해보세요",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                color = Cyan,
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Button(
                                onClick = onGoogleSignIn,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Cyan,
                                    contentColor = DarkBackground
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Google로 로그인",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }

                        if (uiState.error != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.error,
                                color = com.boancurator.app.ui.theme.Error,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        if (uiState.recommendations.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "맞춤 추천",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "활동 기반 개인화 추천 기사",
                    color = TextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.recommendations) { article ->
                        RecommendationCard(article = article)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(article: CardView) {
    Card(
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                LevelBadge(level = article.level)
                Spacer(modifier = Modifier.width(8.dp))
                Text(article.source, color = TextMuted, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = article.title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!article.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = article.summary,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
