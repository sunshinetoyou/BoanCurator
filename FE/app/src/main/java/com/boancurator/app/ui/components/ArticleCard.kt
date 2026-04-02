package com.boancurator.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.boancurator.app.data.model.CardView
import com.boancurator.app.ui.screens.home.getSecurityField
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.DarkCardBorder
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.NeonGreen
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArticleCard(
    article: CardView,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(0.5.dp, NeonGreen.copy(alpha = 0.08f), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard)
    ) {
        Column {
            val imageUrl = article.imageUrls?.firstOrNull()
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = article.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f)
                        .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                        .background(DarkSurface)
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LevelBadge(level = article.level)
                    val field = getSecurityField(article.themes)
                    Text(
                        text = field,
                        color = if (field == "ETC") TextMuted else NeonGreen.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = article.title,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!article.summary.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = article.summary,
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = article.source, color = NeonGreen.copy(alpha = 0.5f), fontSize = 11.sp)
                    Text(text = formatDate(article.publishedAt), color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun formatDate(dateStr: String?): String {
    if (dateStr == null) return ""
    return try {
        dateStr.take(10).replace("-", ".")
    } catch (_: Exception) {
        dateStr
    }
}
