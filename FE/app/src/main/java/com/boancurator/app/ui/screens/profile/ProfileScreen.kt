package com.boancurator.app.ui.screens.profile

import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Slider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
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
import androidx.compose.ui.graphics.Color
import com.boancurator.app.data.model.ApiTheme
import com.boancurator.app.data.model.Keyword
import com.boancurator.app.data.model.NotificationLog
import com.boancurator.app.data.model.NotificationSettingsUpdate
import com.boancurator.app.data.model.Source
import com.boancurator.app.data.model.SourceUpdateRequest
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val domainLabels = mapOf(
    "network_infra" to "네트워크/인프라",
    "malware_vuln" to "악성코드/취약점",
    "cloud_devsecops" to "클라우드/DevSecOps",
    "crypto_auth" to "암호/인증",
    "policy_compliance" to "정책/컴플라이언스",
    "general_it" to "일반 IT"
)

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

    // 탭 진입 시 최신 데이터 로드
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshStats()
    }

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
                            Text(uiState.error!!, color = com.boancurator.app.ui.theme.Error, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // === Profile Edit Dialog ===
    if (showEditDialog) {
        var editName by remember { mutableStateOf(uiState.user?.name ?: "") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = DarkCard,
            title = { Text("프로필 편집", color = TextPrimary) },
            text = {
                TextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("닉네임", color = TextMuted) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Cyan,
                        focusedIndicatorColor = Cyan,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateProfile(editName.trim())
                            showEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground)
                ) { Text("저장") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }

    // === Add Source Dialog ===
    if (showAddSourceDialog) {
        var sourceUrl by remember { mutableStateOf("") }
        var sourceName by remember { mutableStateOf("") }
        val testResult = uiState.sourceTestResult

        AlertDialog(
            onDismissRequest = {
                showAddSourceDialog = false
                viewModel.clearSourceTestResult()
            },
            containerColor = DarkCard,
            title = { Text("소스 추가", color = TextPrimary) },
            text = {
                Column {
                    TextField(
                        value = sourceUrl,
                        onValueChange = { sourceUrl = it },
                        label = { Text("RSS 피드 URL", color = TextMuted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Cyan,
                            focusedIndicatorColor = Cyan,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    TextField(
                        value = sourceName,
                        onValueChange = { sourceName = it },
                        label = { Text("소스 이름 (선택)", color = TextMuted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Cyan,
                            focusedIndicatorColor = Cyan,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    if (uiState.isSourceTesting) {
                        CircularProgressIndicator(color = Cyan, modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally))
                    }

                    if (testResult != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (testResult.valid) "유효한 피드 (${testResult.sampleCount}개 기사 감지)"
                                   else testResult.message ?: "유효하지 않은 피드",
                            color = if (testResult.valid) com.boancurator.app.ui.theme.Success else com.boancurator.app.ui.theme.Error,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { if (sourceUrl.isNotBlank()) viewModel.testSource(sourceUrl.trim()) }
                    ) { Text("테스트", color = Cyan) }

                    if (testResult?.valid == true) {
                        Button(
                            onClick = {
                                viewModel.createSource(sourceUrl.trim(), sourceName.ifBlank { null })
                                showAddSourceDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground)
                        ) { Text("등록") }
                    }
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showAddSourceDialog = false
                    viewModel.clearSourceTestResult()
                }) { Text("취소", color = TextSecondary) }
            }
        )
    }
    // === Add Keyword Dialog ===
    if (showAddKeywordDialog) {
        var keywordText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddKeywordDialog = false },
            containerColor = DarkCard,
            title = { Text("키워드 추가", color = TextPrimary) },
            text = {
                TextField(
                    value = keywordText,
                    onValueChange = { keywordText = it },
                    label = { Text("키워드", color = TextMuted) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkSurface,
                        unfocusedContainerColor = DarkSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Cyan,
                        focusedIndicatorColor = Cyan,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (keywordText.isNotBlank()) {
                            viewModel.createKeyword(keywordText.trim())
                            showAddKeywordDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground)
                ) { Text("추가") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddKeywordDialog = false }) {
                    Text("취소", color = TextSecondary)
                }
            }
        )
    }
}

// === Reusable Section Components ===

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextMuted, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeChips(allThemes: List<String>, selectedThemes: List<String>, onToggle: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        allThemes.forEach { theme ->
            val selected = theme in selectedThemes
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selected) Cyan.copy(alpha = 0.15f) else DarkCard)
                    .border(1.dp, if (selected) Cyan.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(20.dp))
                    .clickable { onToggle(theme) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = theme,
                    color = if (selected) Cyan else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeywordChips(keywords: List<Keyword>, onDelete: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        keywords.forEach { kw ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkCard)
                    .border(1.dp, Cyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(kw.keyword, color = TextPrimary, fontSize = 13.sp)
                    IconButton(onClick = { onDelete(kw.id) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, "삭제", tint = TextMuted, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsSection(
    settings: com.boancurator.app.data.model.NotificationSettings,
    onUpdate: (NotificationSettingsUpdate) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Mode
            Text("알림 모드", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("realtime" to "실시간", "daily" to "일간 요약").forEach { (value, label) ->
                    val selected = settings.mode == value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Cyan.copy(alpha = 0.15f) else DarkSurface)
                            .clickable { onUpdate(NotificationSettingsUpdate(mode = value)) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (selected) Cyan else TextMuted, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Match preset
            Text("매칭 강도", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("strict" to "엄격", "normal" to "보통", "loose" to "느슨").forEach { (value, label) ->
                    val selected = settings.matchPreset == value
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Cyan.copy(alpha = 0.15f) else DarkSurface)
                            .clickable { onUpdate(NotificationSettingsUpdate(matchPreset = value)) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (selected) Cyan else TextMuted, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Daily limit
            Text("하루 알림 제한: ${settings.dailyLimit ?: 10}건", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = (settings.dailyLimit ?: 10).toFloat(),
                onValueChange = {},
                onValueChangeFinished = {},
                valueRange = 1f..20f,
                steps = 18,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Cyan
                )
            )

            // Top N
            Text("추천 건수: ${settings.topN ?: 3}건", color = TextSecondary, fontSize = 12.sp)
            Slider(
                value = (settings.topN ?: 3).toFloat(),
                onValueChange = {},
                onValueChangeFinished = {},
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Cyan
                )
            )
        }
    }
}

@Composable
private fun NotificationLogCard(log: NotificationLog) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Notifications, null, tint = Cyan.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.title ?: "", color = TextPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!log.message.isNullOrBlank()) {
                    Text(log.message!!, color = TextMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (log.createdAt != null) {
                Text(log.createdAt!!.take(10), color = TextMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SourceCard(source: Source, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.RssFeed, null, tint = if (source.enabled) Cyan else TextMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    source.sourceName ?: source.url,
                    color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    source.url, color = TextMuted, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (source.isSystem) {
                    Text("시스템 소스", color = TextMuted, fontSize = 10.sp)
                }
            }
            if (!source.isSystem) {
                Switch(
                    checked = source.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Cyan,
                        checkedTrackColor = Cyan.copy(alpha = 0.3f),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkSurface
                    ),
                    modifier = Modifier.size(36.dp)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, "삭제", tint = TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ExpertiseRadar(expertise: Map<String, Double>, modifier: Modifier) {
    val entries = expertise.entries.toList()
    val maxVal = 5.0 // 5점 만점 고정
    val cyanColor = Cyan

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = min(cx, cy) * 0.55f
        val n = entries.size
        if (n < 3) return@Canvas

        val angleStep = 2 * PI / n
        val startAngle = -PI / 2

        // Grid lines
        for (ring in 1..4) {
            val r = radius * ring / 4
            val path = Path()
            for (i in 0..n) {
                val angle = startAngle + angleStep * (i % n)
                val x = cx + r * cos(angle).toFloat()
                val y = cy + r * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, cyanColor.copy(alpha = 0.08f), style = Stroke(width = 1f))
        }

        // Axis lines
        for (i in 0 until n) {
            val angle = startAngle + angleStep * i
            drawLine(
                cyanColor.copy(alpha = 0.1f),
                Offset(cx, cy),
                Offset(cx + radius * cos(angle).toFloat(), cy + radius * sin(angle).toFloat()),
                strokeWidth = 1f
            )
        }

        // Data polygon
        val dataPath = Path()
        for (i in entries.indices) {
            val ratio = (entries[i].value / maxVal).toFloat().coerceIn(0f, 1f)
            val r = radius * ratio
            val angle = startAngle + angleStep * i
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, cyanColor.copy(alpha = 0.15f))
        drawPath(dataPath, cyanColor.copy(alpha = 0.6f), style = Stroke(width = 2f))

        // Data points + labels
        for (i in entries.indices) {
            val ratio = (entries[i].value / maxVal).toFloat().coerceIn(0f, 1f)
            val r = radius * ratio
            val angle = startAngle + angleStep * i
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            drawCircle(cyanColor, 4f, Offset(x, y))

            // Label
            val labelR = radius + 32f
            val lx = cx + labelR * cos(angle).toFloat()
            val ly = cy + labelR * sin(angle).toFloat()
            val label = domainLabels[entries[i].key] ?: entries[i].key
            drawContext.canvas.nativeCanvas.drawText(
                label, lx, ly + 5f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#8B949E")
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

@Composable
private fun DomainBar(label: String, value: Double, maxValue: Double) {
    val ratio = (value / maxValue).toFloat().coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Box(
            modifier = Modifier.weight(1f).height(8.dp)
                .clip(RoundedCornerShape(4.dp)).background(DarkSurface)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(ratio).height(8.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Cyan.copy(alpha = 0.7f))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("%.1f".format(value), color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp))
    }
}

private fun getTopDomain(distribution: Map<String, Int>): String {
    val top = distribution.maxByOrNull { it.value }
    return if (top != null && top.value > 0) {
        domainLabels[top.key]?.take(8) ?: top.key
    } else "없음"
}
