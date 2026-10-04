package com.example.appfinancetest.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.calculations.PercentageText
import com.example.appfinancetest.calculations.shimmerLoadingAnimation

@Composable
fun EvolutionItem(label: String, evolution: Double?, isLoading: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
        if (isLoading) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(40.dp, 16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerLoadingAnimation()
            )
        } else if (evolution == null) {
            Text(
                text = "N/A",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            PercentageText(
                amount = evolution,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}