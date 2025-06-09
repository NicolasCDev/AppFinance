package com.example.appfinancetest

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.platform.LocalDensity

@Composable
fun LineChartPager(
    databaseViewModel: DataBase_ViewModel,
    investmentViewModel: InvestmentDB_ViewModel,
    range: ClosedFloatingPointRange<Float>
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val heightDp = 230.dp
    val density = LocalDensity.current
    val heightPx = with(density) { heightDp.toPx() }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return if (available.x != 0f) Offset.Zero else available // Autorise uniquement le swipe horizontal
            }
        }
    }
    var userScrollEnabled by remember {
        mutableStateOf(true)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .padding(4.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            Log.d("LineChartPager", "Drag detected: $dragAmount")
                            Log.d("LineChartPager", "Change: $change")
                            val y = change.position.y
                            Log.d("LineChartPager", "Y: $y")
                            // Define the zones: top 25%, middle 50%, bottom 25%
                            val topZoneEnd = heightPx * 0.25f
                            Log.d("LineChartPager", "Top zone end: $topZoneEnd")
                            val bottomZoneStart = heightPx * 0.75f
                            Log.d("LineChartPager", "Bottom zone start: $bottomZoneStart")

                            // Allow pager swipe only if in top or bottom zone
                            if (y < topZoneEnd || y > bottomZoneStart) {
                                Log.d("LineChartPager", "Drag allowed")
                                // Propagate the drag event to the Pager
                                //change.consume()
                            } else {
                                Log.d("LineChartPager", "Drag not allowed")
                                // Consume the drag event if in the middle zone to prevent Pager swipe
                                change.consume()
                                Log.d("LineChartPager", "Change: $change")
                            }
                        }
                    )
                }

        ) { page ->
            when (page) {
                0 -> BalanceLineChart(
                    viewModel = databaseViewModel,
                    startDate = range.start.toDouble(),
                    endDate = range.endInclusive.toDouble()
                )
                1 -> InvestmentLineChart(
                    databaseViewModel = databaseViewModel,
                    investmentViewModel = investmentViewModel,
                    startDate = range.start.toDouble(),
                    endDate = range.endInclusive.toDouble()
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            repeat(2) { index ->
                val selected = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(if (selected) 12.dp else 8.dp)
                        .padding(4.dp)
                        .background(
                            color = if (selected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable {
                            coroutineScope.launch {
                                pagerState.scrollToPage(index)
                            }
                        }
                )
            }
        }
    }
}


