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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.boancurator.app.BuildConfig
import com.boancurator.app.data.model.ApiTheme
import com.boancurator.app.data.model.SourceUpdateRequest
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.Error
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

    var showEditDialog by remember { mutableStateOf(false) }
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }

    val onGoogleSignIn: () -> Unit = {
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val signInOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_CLIENT_ID).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
                val result = credentialManager.getCredential(context, request)
                val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
                viewModel.loginWithGoogle(googleIdToken.idToken)
            } catch (e: Exception) {
                Log.e("ProfileScreen", "Google Sign-In failed", e)
            }
        }
    }

    LaunchedEffect(Unit) { viewModel.refreshStats() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(DarkBackground),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Logout button (top right)
        item {
            if (uiState.isLoggedIn) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "로그아웃", tint = TextMuted)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (uiState.isLoggedIn && uiState.user != null) {
            // === User Profile Card ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.user!!.picture != null) {
                            AsyncImage(
                                model = uiState.user!!.picture,
                                contentDescription = "프로필",
                                modifier = Modifier.size(56.dp).clip(CircleShape)
                                    .border(2.dp, Cyan.copy(alpha = 0.3f), CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier.size(56.dp).clip(CircleShape).background(DarkSurface)
                                    .border(2.dp, Cyan.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Person, null, tint = TextMuted, modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.user?.name ?: "사용자", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(uiState.user?.email ?: "", color = TextSecondary, fontSize = 14.sp)
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Filled.Edit, "프로필 편집", tint = TextMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // === Stats ===
            item {
                Spacer(Modifier.height(16.dp))
                if (uiState.stats != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            label = "북마크",
                            value = "${uiState.stats!!.bookmarkCount}",
                            icon = Icons.Filled.Bookmark,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "주요 관심",
                            value = getTopDomain(uiState.stats!!.domainDistribution),
                            icon = Icons.Filled.Shield,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // === Expertise Radar ===
            item {
                Spacer(Modifier.height(20.dp))
                if (uiState.expertise != null && uiState.expertise!!.isNotEmpty()) {
                    Text(
                        "보안 전문성",
                        color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "활동 기반 도메인별 전문성 분석",
                        color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        ExpertiseRadar(
                            expertise = uiState.expertise!!,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.2f).padding(24.dp)
                        )
                    }
                }
            }

            // === Domain breakdown ===
            item {
                if (uiState.expertise != null) {
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            uiState.expertise!!.entries.sortedByDescending { it.value }.forEach { (key, value) ->
                                val label = domainLabels[key] ?: key
                                DomainBar(label = label, value = value, maxValue = 5.0)
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // === Interest Themes ===
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader("관심 테마", "추천 및 알림에 반영됩니다")
                Spacer(Modifier.height(8.dp))
                ThemeChips(
                    allThemes = ApiTheme.all,
                    selectedThemes = uiState.themes,
                    onToggle = { viewModel.toggleTheme(it) }
                )
            }

            // === Keywords ===
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("키워드 알림", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("등록한 키워드와 관련된 기사가 알림됩니다", color = TextMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { showAddKeywordDialog = true }) {
                        Icon(Icons.Filled.Add, "키워드 추가", tint = Cyan, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (uiState.keywords.isEmpty()) {
                    Text("등록된 키워드가 없습니다", color = TextMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp))
                } else {
                    KeywordChips(
                        keywords = uiState.keywords,
                        onDelete = { viewModel.deleteKeyword(it) }
                    )
                }
            }

            // === Notification Settings ===
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader("알림 설정", "푸시 알림 방식을 설정합니다")
                Spacer(Modifier.height(8.dp))
                if (uiState.notificationSettings != null) {
                    NotificationSettingsSection(
                        settings = uiState.notificationSettings!!,
                        onUpdate = { viewModel.updateNotificationSettings(it) }
                    )
                }
            }

            // === Notification Log ===
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader("알림 이력", "최근 수신한 알림")
                Spacer(Modifier.height(8.dp))
                if (uiState.notificationLog.isEmpty()) {
                    Text("알림 이력이 없습니다", color = TextMuted, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
            items(uiState.notificationLog.take(10), key = { it.id ?: it.hashCode() }) { log ->
                NotificationLogCard(log)
            }

            // === Source Management ===
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("소스 관리", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("RSS 피드 소스 추가 및 관리", color = TextMuted, fontSize = 12.sp)
                    }
                    IconButton(onClick = { showAddSourceDialog = true }) {
                        Icon(Icons.Filled.Add, "소스 추가", tint = Cyan, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            items(uiState.sources, key = { it.id ?: it.url }) { source ->
                SourceCard(
                    source = source,
                    onToggle = { enabled ->
                        source.id?.let { viewModel.updateSource(it, SourceUpdateRequest(enabled = enabled)) }
                    },
                    onDelete = { if (!source.isSystem) source.id?.let { viewModel.deleteSource(it) } }
                )
            }
        } else {
            // === Login prompt ===
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Shield, null, tint = Cyan, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("보안큐레이터에 로그인", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("북마크, 개인화 추천, 전문성 분석 등\n더 많은 기능을 이용해보세요",
                            color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(24.dp))
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = Cyan, modifier = Modifier.size(32.dp))
                        } else {
                            Button(
                                onClick = onGoogleSignIn,
                                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Google로 로그인", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                        if (uiState.error != null) {
                            Spacer(Modifier.height(12.dp))
                            Text(uiState.error!!, color = Error, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        ProfileEditDialog(
            initialName = uiState.user?.name ?: "",
            onConfirm = { name ->
                viewModel.updateProfile(name)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
    }

    if (showAddSourceDialog) {
        AddSourceDialog(
            isSourceTesting = uiState.isSourceTesting,
            sourceTestResult = uiState.sourceTestResult,
            onTest = { url -> viewModel.testSource(url) },
            onCreate = { url, name ->
                viewModel.createSource(url, name)
                showAddSourceDialog = false
            },
            onDismiss = {
                showAddSourceDialog = false
                viewModel.clearSourceTestResult()
            }
        )
    }

    if (showAddKeywordDialog) {
        AddKeywordDialog(
            onConfirm = { kw ->
                viewModel.createKeyword(kw)
                showAddKeywordDialog = false
            },
            onDismiss = { showAddKeywordDialog = false }
        )
    }
}
