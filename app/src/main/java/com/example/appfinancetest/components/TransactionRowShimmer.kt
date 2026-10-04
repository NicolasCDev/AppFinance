package com.example.appfinancetest.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.calculations.shimmerLoadingAnimation

@Composable
fun TransactionRowShimmer() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(150.dp, 20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerLoadingAnimation()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(200.dp, 14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerLoadingAnimation()
            )
        }
        Box(
            modifier = Modifier
                .size(60.dp, 20.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerLoadingAnimation()
        )
    }
}