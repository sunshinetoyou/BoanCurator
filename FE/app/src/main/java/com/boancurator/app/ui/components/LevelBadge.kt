package com.boancurator.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boancurator.app.data.model.ApiLevel
import com.boancurator.app.ui.theme.LevelAdvanced
import com.boancurator.app.ui.theme.LevelBeginner
import com.boancurator.app.ui.theme.LevelIntermediate
import com.boancurator.app.ui.theme.TextPrimary

@Composable
fun LevelBadge(level: String?, modifier: Modifier = Modifier) {
    if (level == null) return

    val (color, label) = when (level) {
        ApiLevel.LOW -> LevelBeginner to "초급"
        ApiLevel.MEDIUM -> LevelIntermediate to "중급"
        ApiLevel.HIGH -> LevelAdvanced to "고급"
        else -> LevelBeginner to level
    }

    Text(
        text = label,
        color = TextPrimary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
