package com.boancurator.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boancurator.app.ui.theme.ChipSelected
import com.boancurator.app.ui.theme.ChipUnselected
import com.boancurator.app.ui.theme.DarkCardBorder
import com.boancurator.app.ui.theme.NeonGreen
import com.boancurator.app.ui.theme.TextMuted

@Composable
fun LabeledFilterChipRow(
    items: List<String>,
    selectedItem: String?,
    onItemSelected: (String?) -> Unit,
    labelMapper: (String) -> String = { it },
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        FilterChip(
            selected = selectedItem == null,
            onClick = { onItemSelected(null) },
            label = { Text("ALL", fontSize = 12.sp) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = ChipSelected,
                selectedLabelColor = NeonGreen,
                containerColor = ChipUnselected,
                labelColor = TextMuted
            ),
            border = FilterChipDefaults.filterChipBorder(
                borderColor = DarkCardBorder,
                selectedBorderColor = NeonGreen.copy(alpha = 0.4f),
                enabled = true,
                selected = selectedItem == null
            )
        )

        items.forEach { item ->
            FilterChip(
                selected = selectedItem == item,
                onClick = { onItemSelected(item) },
                label = { Text(labelMapper(item), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ChipSelected,
                    selectedLabelColor = NeonGreen,
                    containerColor = ChipUnselected,
                    labelColor = TextMuted
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = DarkCardBorder,
                    selectedBorderColor = NeonGreen.copy(alpha = 0.4f),
                    enabled = true,
                    selected = selectedItem == item
                )
            )
        }
    }
}
