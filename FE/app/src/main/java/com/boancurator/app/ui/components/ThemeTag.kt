package com.boancurator.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkCard

@Composable
fun ThemeTag(theme: String, modifier: Modifier = Modifier) {
    Text(
        text = "#$theme",
        color = Cyan,
        fontSize = 11.sp,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DarkCard)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
