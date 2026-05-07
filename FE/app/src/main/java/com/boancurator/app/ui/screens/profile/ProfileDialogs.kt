package com.boancurator.app.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boancurator.app.data.model.SourceTestResult
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkBackground
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.Error
import com.boancurator.app.ui.theme.Success
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary

@Composable
internal fun ProfileEditDialog(
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var editName by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("프로필 편집", color = TextPrimary) },
        text = {
            TextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("닉네임", color = TextMuted) },
                singleLine = true,
                colors = profileTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editName.isNotBlank()) onConfirm(editName.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground)
            ) { Text("저장") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun AddKeywordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var keywordText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("키워드 추가", color = TextPrimary) },
        text = {
            TextField(
                value = keywordText,
                onValueChange = { keywordText = it },
                label = { Text("키워드", color = TextMuted) },
                singleLine = true,
                colors = profileTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (keywordText.isNotBlank()) onConfirm(keywordText.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground)
            ) { Text("추가") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("취소", color = TextSecondary)
            }
        }
    )
}

@Composable
internal fun AddSourceDialog(
    isSourceTesting: Boolean,
    sourceTestResult: SourceTestResult?,
    onTest: (String) -> Unit,
    onCreate: (String, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var sourceUrl by remember { mutableStateOf("") }
    var sourceName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("소스 추가", color = TextPrimary) },
        text = {
            Column {
                TextField(
                    value = sourceUrl,
                    onValueChange = { sourceUrl = it },
                    label = { Text("RSS 피드 URL", color = TextMuted) },
                    singleLine = true,
                    colors = profileTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = sourceName,
                    onValueChange = { sourceName = it },
                    label = { Text("소스 이름 (선택)", color = TextMuted) },
                    singleLine = true,
                    colors = profileTextFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))

                if (isSourceTesting) {
                    CircularProgressIndicator(
                        color = Cyan,
                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally)
                    )
                }

                if (sourceTestResult != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (sourceTestResult.valid) "유효한 피드 (${sourceTestResult.sampleCount}개 기사 감지)"
                               else sourceTestResult.message ?: "유효하지 않은 피드",
                        color = if (sourceTestResult.valid) Success else Error,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (sourceUrl.isNotBlank()) onTest(sourceUrl.trim()) }
                ) { Text("테스트", color = Cyan) }

                if (sourceTestResult?.valid == true) {
                    Button(
                        onClick = { onCreate(sourceUrl.trim(), sourceName.ifBlank { null }) },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = DarkBackground)
                    ) { Text("등록") }
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("취소", color = TextSecondary) }
        }
    )
}

@Composable
private fun profileTextFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = DarkSurface,
    unfocusedContainerColor = DarkSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Cyan,
    focusedIndicatorColor = Cyan,
    unfocusedIndicatorColor = Color.Transparent
)
