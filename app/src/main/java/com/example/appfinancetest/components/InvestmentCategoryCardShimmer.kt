package com.example.appfinancetest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.shimmerLoadingAnimation

@Composable
fun InvestmentCategoryCardShimmer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(120.dp, 24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerLoadingAnimation()
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(100.dp, 16.dp).clip(RoundedCornerShape(4.dp)).shimmerLoadingAnimation())
                    Box(modifier = Modifier.size(100.dp, 16.dp).clip(RoundedCornerShape(4.dp)).shimmerLoadingAnimation())
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.size(80.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmerLoadingAnimation())
                    Box(modifier = Modifier.size(60.dp, 20.dp).clip(RoundedCornerShape(4.dp)).shimmerLoadingAnimation())
                    Box(modifier = Modifier.size(40.dp, 14.dp).clip(RoundedCornerShape(4.dp)).shimmerLoadingAnimation())
                }
            }
        }
    }
}