package com.example.appfinancetest.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.views.PortfolioSlice
import kotlin.collections.forEach

@Composable
fun HomeDonutChart(slices: List<PortfolioSlice>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 18.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        val sizeArc = Size(diameter, diameter)

        var startAngle = -90f
        slices.forEach { slice ->
            val sweepAngle = (slice.percentage / 100f) * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f, // Small gap between segments
                useCenter = false,
                topLeft = topLeft,
                size = sizeArc,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}
