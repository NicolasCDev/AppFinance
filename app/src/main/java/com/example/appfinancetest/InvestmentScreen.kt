package com.example.appfinancetest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

@Composable
fun InvestmentScreen(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    creditViewModel: CreditDBViewModel
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    // State for the detail view
    var selectedCategoryForDetail by remember { mutableStateOf<String?>(null) }

    // Collecting every investment with refreshTrigger
    val allInvestments by produceState(initialValue = emptyList(), investmentViewModel, refreshTrigger) {
        value = investmentViewModel.getInvestment()
    }

    // Dynamic extraction of unique items in BDD the database
    val dynamicItems = remember(allInvestments) {
        allInvestments.mapNotNull { it.item }.filter { it.isNotBlank() }.distinct().sorted()
    }

    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
    if (showImportExport) {
        ImportExportInterface(
            databaseViewModel = databaseViewModel,
            investmentViewModel = investmentViewModel,
            creditViewModel = creditViewModel,
            onDismiss = { showImportExport = false },
            onRefresh = {
                refreshTrigger++
                databaseViewModel.refreshNetWorth()
            }
        )
    }
    
    // Detailed view dialog
    selectedCategoryForDetail?.let { category ->
        InvestmentDetailDialog(
            category = category,
            investments = allInvestments.filter { it.item == category },
            databaseViewModel = databaseViewModel,
            investmentViewModel = investmentViewModel,
            isVisibilityOff = isVisibilityOff,
            onRefresh = { refreshTrigger++ },
            onDismiss = { selectedCategoryForDetail = null }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onSettingsClick = { showSettings = true },
                onImportExportClick = { showImportExport = true },
                onVisibilityClick = {
                    scope.launch { prefs.saveVisibilityState(!isVisibilityOff) }
                },
                isVisibilityOff = isVisibilityOff,
                name = stringResource(id = R.string.investments_title)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (dynamicItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        stringResource(id = R.string.no_investments_found),
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    ImportActionCard(
                        databaseViewModel = databaseViewModel,
                        investmentViewModel = investmentViewModel,
                        creditViewModel = creditViewModel,
                        onRefresh = { refreshTrigger++ }
                    )
                }
            } else {
                dynamicItems.forEach { itemKey ->
                    val investmentsInCategory = allInvestments.filter { it.item == itemKey }
                    
                    val ongoing = investmentsInCategory.filter { it.dateEnd == null || it.dateEnd == 0.0 }
                    val finished = investmentsInCategory.filter { it.dateEnd != null && it.dateEnd != 0.0 }
                    
                    val sumOngoing = ongoing.sumOf { it.invested ?: 0.0 }
                    val sumFinishedInvested = finished.sumOf { it.invested ?: 0.0 }
                    val sumFinishedEarned = finished.sumOf { it.earned ?: 0.0 }
                    
                    val profitEuro = sumFinishedEarned - sumFinishedInvested
                    val profitPercent = if (sumFinishedInvested > 0) (profitEuro / sumFinishedInvested) * 100 else 0.0

                    // Weighted annual profitability calculation
                    val totalInvested = investmentsInCategory.sumOf { it.invested ?: 0.0 }
                    val weightedAnnualProfitability = if (totalInvested > 0.0) {
                        investmentsInCategory.sumOf { (it.annualProfitability ?: 0.0) * (it.invested ?: 0.0) } / totalInvested
                    } else 0.0

                    InvestmentCategoryCard(
                        title = itemKey,
                        sumOngoing = sumOngoing,
                        sumFinished = sumFinishedInvested,
                        profitEuro = profitEuro,
                        profitPercent = profitPercent,
                        weightedAnnualProfitability = weightedAnnualProfitability,
                        isVisibilityOff = isVisibilityOff,
                        onClick = { selectedCategoryForDetail = itemKey }
                    )
                }
            }
        }
    }
}

@Composable
fun InvestmentCategoryCard(
    title: String,
    sumOngoing: Double,
    sumFinished: Double,
    profitEuro: Double,
    profitPercent: Double,
    weightedAnnualProfitability: Double,
    isVisibilityOff: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    LabelValue(label = stringResource(id = R.string.investment_current), value = if (isVisibilityOff) "**** €" else "%.2f €".format(sumOngoing))
                    LabelValue(label = stringResource(id = R.string.investment_closed), value = if (isVisibilityOff) "**** €" else "%.2f €".format(sumFinished))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(id = R.string.capital_gain_realized), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = if (isVisibilityOff) "**** €" else "${if (profitEuro >= 0) "+" else ""}${"%.2f".format(profitEuro)} €",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (profitEuro >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                    Text(
                        text = "(${if (profitPercent >= 0) "+" else ""}${"%.1f".format(profitPercent)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (profitPercent >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(id = R.string.annual_profitability), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        text = "${if (weightedAnnualProfitability >= 0) "+" else ""}${"%.1f".format(weightedAnnualProfitability)} % (${stringResource(id = R.string.annual)})",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (weightedAnnualProfitability >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentDetailDialog(
    category: String,
    investments: List<InvestmentDB>,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    isVisibilityOff: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.investment_current),
        stringResource(id = R.string.investment_closed)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val filteredInvestments = remember(investments, selectedTabIndex) {
                    if (selectedTabIndex == 0) {
                        investments.filter { it.dateEnd == null || it.dateEnd == 0.0 }
                    } else {
                        investments.filter { it.dateEnd != null && it.dateEnd != 0.0 }
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredInvestments.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(id = R.string.no_investment), color = Color.Gray)
                            }
                        }
                    } else {
                        // FIX: Key is modified to include selectedTabIndex to prevent state reuse between tabs
                        items(filteredInvestments, key = { "${it.id}_$selectedTabIndex" }) { investment ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd && selectedTabIndex == 0) true
                                    else value == SwipeToDismissBoxValue.EndToStart && selectedTabIndex == 1
                                }
                            )

                            // Effect to handle the actual database update after the swipe is confirmed
                            LaunchedEffect(dismissState.currentValue) {
                                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd && selectedTabIndex == 0) {
                                    validateInvestments(databaseViewModel, investmentViewModel, investment.idInvest) {
                                        databaseViewModel.refreshNetWorth()
                                        onRefresh()
                                    }
                                } else if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart && selectedTabIndex == 1) {
                                    invalidateInvestments(databaseViewModel, investmentViewModel, investment.idInvest) {
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
                                PositionItemCard(investment, isVisibilityOff)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PositionItemCard(investment: InvestmentDB, isVisibilityOff: Boolean) {
    val invested = investment.invested ?: 0.0
    val earned = investment.earned ?: 0.0
    val profitEuro = earned - invested
    val profitPercent = if (invested > 0) (profitEuro / invested) * 100 else 0.0
    val annual = investment.annualProfitability ?: 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = investment.label ?: stringResource(id = R.string.no_label),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isVisibilityOff) "**** €" else "${if (profitEuro >= 0) "+" else ""}${"%.2f".format(profitEuro)} €",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (profitEuro >= 0) Color(0xFF4CAF50) else Color.Red
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "(${if (profitPercent >= 0) "+" else ""}${"%.1f".format(profitPercent)}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (profitPercent >= 0) Color(0xFF4CAF50) else Color.Red
                        )
                    }
                    Text(
                        text = "${if (annual >= 0) "+" else ""}${"%.1f".format(annual)} % (${stringResource(id = R.string.annual)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (annual >= 0) Color(0xFF4CAF50) else Color.Red
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                InfoLabelSmall(label = stringResource(id = R.string.invested), value = if (isVisibilityOff) "**** €" else "%.2f €".format(invested))
                InfoLabelSmall(label = stringResource(id = R.string.earned), value = if (isVisibilityOff) "**** €" else "%.2f €".format(earned))
            }
        }
    }
}

@Composable
fun InfoLabelSmall(label: String, value: String) {
    Row {
        Text("$label: ", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun LabelValue(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
