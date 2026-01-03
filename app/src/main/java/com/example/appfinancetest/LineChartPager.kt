package com.example.appfinancetest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalDensity


@Composable
fun LineChartPager(
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    range: ClosedFloatingPointRange<Float>,
    hideMarkerTrigger: Int = 0,
    onHideMarkers: (() -> Unit)? = null
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val heightDp = 230.dp
    val density = LocalDensity.current
    val heightPx = with(density) { heightDp.toPx() }

    var userScrollEnabled by remember {
        mutableStateOf(true)
    }
    

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = userScrollEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(heightDp)
                .padding(4.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.first()
                            val y = change.position.y
                            
                            // Define the zones: top 25%, middle 50%, bottom 25%
                            val topZoneEnd = heightPx * 0.25f
                            val bottomZoneStart = heightPx * 0.75f
                            
                            when (event.type) {
                                PointerEventType.Press -> {
                                    // Determine if we're in the middle zone
                                    val inMiddleZone = y in topZoneEnd..bottomZoneStart
                                    
                                    // Disable pager scrolling if in middle zone
                                    userScrollEnabled = !inMiddleZone
                                    
                                    // Don't consume the event - let it pass through to the charts
                                }
                                PointerEventType.Release -> {
                                    // Re-enable pager scrolling when touch is released
                                    userScrollEnabled = true
                                    
                                    // Don't consume the event - let it pass through to the charts
                                }
                                // Don't handle Move events at all - let them pass through to charts
                            }
                        }
                    }
                }

        ) { page ->
            when (page) {
                0 -> BalanceLineChart(
                    viewModel = databaseViewModel,
                    startDate = range.start.toDouble(),
                    endDate = range.endInclusive.toDouble(),
                    hideMarkerTrigger = hideMarkerTrigger,
                    onHideMarkers = onHideMarkers
                )
                1 -> InvestmentLineChart(
                    databaseViewModel = databaseViewModel,
                    investmentViewModel = investmentViewModel,
                    startDate = range.start.toDouble(),
                    endDate = range.endInclusive.toDouble(),
                    hideMarkerTrigger = hideMarkerTrigger,
                    onHideMarkers = onHideMarkers
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


