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
        ApiLevel.LOW -> LevelBeginner to "LOW"
        ApiLevel.MEDIUM -> LevelIntermediate to "MID"
        ApiLevel.HIGH -> LevelAdvanced to "HIGH"
        else -> LevelBeginner to level
    }

    Text(
        text = label,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.sp,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
