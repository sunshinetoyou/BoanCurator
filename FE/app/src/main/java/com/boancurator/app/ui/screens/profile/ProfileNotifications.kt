package com.boancurator.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boancurator.app.data.model.NotificationLog
import com.boancurator.app.data.model.NotificationSettings
import com.boancurator.app.data.model.NotificationSettingsUpdate
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary

@Composable
internal fun NotificationSettingsSection(
    settings: NotificationSettings,
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
                colors = SliderDefaults.colors(
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
                colors = SliderDefaults.colors(
                    thumbColor = Cyan,
                    activeTrackColor = Cyan
                )
            )
        }
    }
}

@Composable
internal fun NotificationLogCard(log: NotificationLog) {
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
