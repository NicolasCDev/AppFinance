package com.example.appfinancetest.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.ui.theme.*
import androidx.compose.ui.unit.sp
import com.example.appfinancetest.views.FlowBarData
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max

@Composable
fun ClusteredColumnChartCard(
    title: String,
    barDataList: List<FlowBarData>,
    selectedId: String,
    isVisibilityOff: Boolean,
    cardBg: Color = MaterialTheme.colorScheme.surface,
    cardBorder: Color = MaterialTheme.colorScheme.outlineVariant,
    textMuted: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    textPrimary: Color = MaterialTheme.colorScheme.onSurface,
    onPeriodClick: (FlowBarData) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (barDataList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Aucune donnée pour cette période", color = textMuted)
                }
            } else {
                val maxVal = remember(barDataList) {
                    val highest = barDataList.maxOfOrNull { max(it.inflows, it.outflows) } ?: 1000.0
                    if (highest <= 0) 1000.0 else highest
                }

                val listState = rememberLazyListState()

                // Scroll to selected item when selectedId changes
                LaunchedEffect(selectedId) {
                    val selectedIndex = barDataList.indexOfFirst { it.id == selectedId }
                    if (selectedIndex >= 0) {
                        listState.animateScrollToItem((selectedIndex - 2).coerceAtLeast(0))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    // Y-Axis Labels
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        val ySteps = listOf(maxVal, maxVal * 0.6, maxVal * 0.3, 0.0)
                        ySteps.forEach { valStep ->
                            val label = if (isVisibilityOff) "***"
                            else if (valStep >= 1000) String.format(Locale.getDefault(), "%.0fk", valStep / 1000)
                            else String.format(Locale.getDefault(), "%.0f", valStep)
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Scrollable Clustered Columns Area
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // Background horizontal grid lines
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(4) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = cardBorder
                                )
                            }
                        }

                        val locale = LocalConfiguration.current.locales[0]

                        // LazyRow of grouped bars
                        LazyRow(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            items(barDataList, key = { it.id }) { item ->
                                val inflowHeightRatio = (item.inflows / maxVal).coerceIn(0.01, 1.0).toFloat()
                                val outflowHeightRatio = (item.outflows / maxVal).coerceIn(0.01, 1.0).toFloat()
                                val isItemSelected = (item.id == selectedId)

                                val displayLabel = remember(item.id, item.label, locale) {
                                    if (item.id.contains("-")) {
                                        val cal = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, item.year)
                                            set(Calendar.MONTH, item.month)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                        val sdf = SimpleDateFormat("MMM", locale)
                                        val monthStr = sdf.format(cal.time).replaceFirstChar {
                                            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                                        }
                                        val shortYear = item.year.toString().takeLast(2)
                                        "$monthStr '$shortYear"
                                    } else {
                                        item.label
                                    }
                                }

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isItemSelected) cardBorder.copy(alpha = 0.5f) else Color.Transparent)
                                        .clickable { onPeriodClick(item) }
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .height(140.dp)
                                            .width(42.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // Inflow Bar (Green)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(inflowHeightRatio)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(GreenAccent)
                                        )

                                        // Outflow Bar (Red)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(outflowHeightRatio)
                                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                                .background(RedAccent)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = displayLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = if (isItemSelected) FontWeight.Bold else FontWeight.Normal
                                        ),
                                        color = if (isItemSelected) textPrimary else textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
