package com.example.appfinancetest.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.calculations.CurrencyText
import com.example.appfinancetest.calculations.CurrencyTextOnPrimary
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.classes.InvestmentDB
import com.example.appfinancetest.calculations.PercentageText
import com.example.appfinancetest.R
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.calculations.calculateProfitPercent
import com.example.appfinancetest.calculations.calculateWeightedAnnualProfitability

enum class InvestmentViewMode {
    HEATMAP, LIST
}

@Composable
fun InvestmentSummaryCard(
    totalInvested: Double,
    isVisibilityOff: Boolean,
    selectedTabIndex: Int,
    filteredInvestments: List<InvestmentDB>,
    allTransactions: List<TransactionDB>,
    viewMode: InvestmentViewMode,
    onViewModeChange: (InvestmentViewMode) -> Unit,
    cardBg: Color,
    cardBorder: Color,
    textMuted: Color,
    bluePill: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(id = R.string.invested),
                    style = MaterialTheme.typography.headlineSmall,
                    color = textMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                CurrencyTextOnPrimary(
                    amount = totalInvested,
                    isVisibilityOff = isVisibilityOff,
                    style = MaterialTheme.typography.titleLarge
                )

                if (selectedTabIndex == 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val totalEarned = filteredInvestments.sumOf { it.earned ?: 0.0 }
                    val profitEuro = totalEarned - totalInvested
                    val profitPercent =
                        calculateProfitPercent(totalInvested, profitEuro + totalInvested)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.capital_gain) + ":",
                            style = MaterialTheme.typography.bodySmall,
                            color = textMuted
                        )
                        CurrencyText(
                            amount = profitEuro,
                            isVisibilityOff = isVisibilityOff,
                            showSign = true,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        PercentageText(
                            amount = profitPercent,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (selectedTabIndex == 0) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(
                            BorderStroke(1.dp, cardBorder),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isHeatmap = viewMode == InvestmentViewMode.HEATMAP
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isHeatmap) bluePill else Color.Transparent)
                            .clickable { onViewModeChange(InvestmentViewMode.HEATMAP) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.heatmap_mode),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isHeatmap) Color.White else textMuted,
                            fontWeight = if (isHeatmap) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    val isList = viewMode == InvestmentViewMode.LIST
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isList) bluePill else Color.Transparent)
                            .clickable { onViewModeChange(InvestmentViewMode.LIST) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(id = R.string.list_mode),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isList) Color.White else textMuted,
                            fontWeight = if (isList) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            } else if (selectedTabIndex == 1) {
                val weightedAnnualProfitability =
                    calculateWeightedAnnualProfitability(filteredInvestments, allTransactions)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(id = R.string.annual_profitability),
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PercentageText(
                            amount = weightedAnnualProfitability,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = " (${stringResource(id = R.string.annual)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = textMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvestmentHeatmapCard(
    investment: InvestmentDB,
    investedAmount: Double,
    isVisibilityOff: Boolean,
    cardBg: Color,
    cardBorder: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = investment.label ?: stringResource(id = R.string.no_label),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )
            CurrencyText(
                amount = investedAmount,
                isVisibilityOff = isVisibilityOff,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun InvestmentListItemCard(
    investment: InvestmentDB,
    isVisibilityOff: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = investment.label ?: stringResource(id = R.string.no_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = textPrimary,
                    textAlign = TextAlign.Start,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Column(horizontalAlignment = Alignment.End) {
                    val invested = investment.invested ?: 0.0
                    val earned = investment.earned ?: 0.0
                    val profitEuro = earned - invested
                    val profitPercent = calculateProfitPercent(invested, earned)
                    val annual = investment.annualProfitability ?: 0.0

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyText(
                            amount = profitEuro,
                            isVisibilityOff = isVisibilityOff,
                            showSign = true,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        PercentageText(
                            amount = profitPercent,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PercentageText(
                            amount = annual,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = " (${stringResource(id = R.string.annual)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = textMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stringResource(id = R.string.invested)}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                    CurrencyTextOnPrimary(
                        amount = investment.invested ?: 0.0,
                        isVisibilityOff = isVisibilityOff,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${stringResource(id = R.string.earned)}: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                    CurrencyTextOnPrimary(
                        amount = investment.earned ?: 0.0,
                        isVisibilityOff = isVisibilityOff,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun InvestmentHeatmapView(
    filteredInvestments: List<InvestmentDB>,
    allTransactions: List<TransactionDB>,
    isVisibilityOff: Boolean,
    cardBg: Color,
    cardBorder: Color,
    onInvestmentClick: (InvestmentDB, List<TransactionDB>) -> Unit,
    modifier: Modifier = Modifier
) {
    val heatmapScrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(heatmapScrollState),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val totalCatInvested = filteredInvestments.sumOf { it.invested ?: 0.0 }.coerceAtLeast(1.0)

        val sortedInvestments = remember(filteredInvestments) {
            filteredInvestments.sortedByDescending { it.invested ?: 0.0 }
        }

        val treemapRows = remember(sortedInvestments, totalCatInvested) {
            val rows = mutableListOf<List<InvestmentDB>>()
            var currentRow = mutableListOf<InvestmentDB>()
            var currentRowSum = 0.0
            val targetRowSum = totalCatInvested * 0.5

            for (inv in sortedInvestments) {
                val invAmt = inv.invested ?: 0.0
                if (currentRow.isEmpty() || (currentRow.size < 3 && currentRowSum + invAmt <= targetRowSum * 1.3)) {
                    currentRow.add(inv)
                    currentRowSum += invAmt
                } else {
                    rows.add(currentRow)
                    currentRow = mutableListOf(inv)
                    currentRowSum = invAmt
                }
            }
            if (currentRow.isNotEmpty()) {
                rows.add(currentRow)
            }
            rows
        }

        treemapRows.forEach { rowInvestments ->
            val rowSum = rowInvestments.sumOf { it.invested ?: 0.0 }.coerceAtLeast(1.0)
            val rowHeight = (150.dp * (rowSum / totalCatInvested).toFloat()).coerceIn(45.dp, 77.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowInvestments.forEach { investment ->
                    val inv = investment.invested ?: 0.0
                    val itemWeight = (inv / rowSum).toFloat().coerceAtLeast(0.15f)

                    InvestmentHeatmapCard(
                        investment = investment,
                        investedAmount = inv,
                        isVisibilityOff = isVisibilityOff,
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        modifier = Modifier
                            .weight(itemWeight)
                            .fillMaxHeight(),
                        onClick = {
                            val transactions = allTransactions.filter { it.idInvest == investment.idInvest }
                            onInvestmentClick(investment, transactions)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InvestmentListView(
    filteredInvestments: List<InvestmentDB>,
    allTransactions: List<TransactionDB>,
    isVisibilityOff: Boolean,
    cardBg: Color,
    cardBorder: Color,
    textPrimary: Color,
    textMuted: Color,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    selectedTabIndex: Int,
    onRefresh: () -> Unit,
    onInvestmentClick: (InvestmentDB, List<TransactionDB>) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filteredInvestments, key = { "${it.id}_$selectedTabIndex" }) { investment ->
            val transactions = allTransactions.filter { it.idInvest == investment.idInvest }

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.StartToEnd && selectedTabIndex == 0) true
                    else value == SwipeToDismissBoxValue.EndToStart && selectedTabIndex == 1
                }
            )

            // Effect to handle the actual database update after the swipe is confirmed
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd && selectedTabIndex == 0) {
                    validateInvestments(
                        databaseViewModel,
                        investmentViewModel,
                        investment.idInvest
                    ) {
                        databaseViewModel.refreshNetWorth()
                        onRefresh()
                    }
                } else if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && selectedTabIndex == 1) {
                    invalidateInvestments(
                        databaseViewModel,
                        investmentViewModel,
                        investment.idInvest
                    ) {
                        databaseViewModel.refreshNetWorth()
                        onRefresh()
                    }
                }
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val isDismissingToEnd = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
                    val isDismissingToStart = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

                    val color = if (isDismissingToEnd) Color(0xFF4CAF50)
                    else if (isDismissingToStart) Color.Gray
                    else Color.Transparent

                    val alignment = if (isDismissingToEnd) Alignment.CenterStart
                    else if (isDismissingToStart) Alignment.CenterEnd
                    else Alignment.Center

                    val icon = if (isDismissingToEnd) Icons.Default.Done
                    else if (isDismissingToStart) Icons.Default.Refresh
                    else null

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color, RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        icon?.let { Icon(it, contentDescription = null, tint = Color.White) }
                    }
                },
                enableDismissFromStartToEnd = selectedTabIndex == 0,
                enableDismissFromEndToStart = selectedTabIndex == 1
            ) {
                InvestmentListItemCard(
                    investment = investment,
                    isVisibilityOff = isVisibilityOff,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary,
                    textMuted = textMuted,
                    onClick = { onInvestmentClick(investment, transactions) }
                )
            }
        }
    }
}
