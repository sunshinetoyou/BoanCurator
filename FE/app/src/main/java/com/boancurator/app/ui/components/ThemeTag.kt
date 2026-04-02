package com.boancurator.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boancurator.app.ui.theme.NeonGreen

@Composable
fun ThemeTag(theme: String, modifier: Modifier = Modifier) {
    Text(
        text = "#$theme",
        color = NeonGreen.copy(alpha = 0.7f),
        fontSize = 11.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(0.5.dp, NeonGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
