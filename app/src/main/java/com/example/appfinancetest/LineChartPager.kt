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

@Composable
fun LineChartPager(
    databaseViewModel: DataBase_ViewModel,
    investmentViewModel: InvestmentDB_ViewModel,
    range: ClosedFloatingPointRange<Float>
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .height(230.dp)
                .padding(4.dp),
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
        val coroutineScope = rememberCoroutineScope()

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
                        .clickable { coroutineScope.launch { pagerState.scrollToPage(index) } }
                )
            }
        }
    }
}
