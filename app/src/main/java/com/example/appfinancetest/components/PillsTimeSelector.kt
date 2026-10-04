package com.example.appfinancetest.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.classes.HomeTimeRange
import com.example.appfinancetest.ui.theme.*

@Composable
fun TimeRangeSelectorPills(
    selectedRange: HomeTimeRange,
    onRangeSelected: (HomeTimeRange) -> Unit,
    modifier: Modifier = Modifier,
    bluePill: Color = BluePill,
    unselectedBg: Color = MaterialTheme.colorScheme.surfaceVariant,
    textMuted: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        HomeTimeRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 2.dp)
                    .height(34.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(if (isSelected) bluePill else unselectedBg)
                    .clickable { onRangeSelected(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = range.labelResId),
                    style = textStyle.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else textStyle.fontWeight,
                        color = if (isSelected) Color.White else textMuted
                    )
                )
            }
        }
    }
}
