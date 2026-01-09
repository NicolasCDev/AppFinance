package com.example.appfinancetest

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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

    var isLoading by remember { mutableStateOf(true) }

    // Collecting every investment with refreshTrigger
    val allInvestments by produceState(initialValue = emptyList(), investmentViewModel, refreshTrigger) {
        isLoading = true
        // Simuler un léger délai pour voir le shimmer ou attendre le retour BDD
        value = investmentViewModel.getInvestment()
        isLoading = false
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
            if (isLoading) {
                repeat(3) {
                    InvestmentCategoryCardShimmer()
                }
            } else if (dynamicItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        stringResource(id = R.string.no_investments_found),
                        style = MaterialTheme.typography.bodyLarge,
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
                style = MaterialTheme.typography.titleLarge
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    LabelValue(label = stringResource(id = R.string.investment_current), amount = sumOngoing, isVisibilityOff = isVisibilityOff)
                    LabelValue(label = stringResource(id = R.string.investment_closed), amount = sumFinished, isVisibilityOff = isVisibilityOff)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text = stringResource(id = R.string.capital_gain_realized), style = MaterialTheme.typography.bodySmall)
                    
                    CurrencyText(
                        amount = profitEuro,
                        isVisibilityOff = isVisibilityOff,
                        showSign = true,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    PercentageText(
                        amount = profitPercent,
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = stringResource(id = R.string.annual_profitability), style = MaterialTheme.typography.bodySmall)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PercentageText(
                            amount = weightedAnnualProfitability,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = " (${stringResource(id = R.string.annual)})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

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
                        style = MaterialTheme.typography.titleLarge
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

                if (filteredInvestments.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(id = R.string.no_investment))
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(filteredInvestments) { invest ->
                            InvestmentItemCard(
                                invest = invest,
                                databaseViewModel = databaseViewModel,
                                investmentViewModel = investmentViewModel,
                                isVisibilityOff = isVisibilityOff,
                                onRefresh = onRefresh
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvestmentItemCard(
    invest: InvestmentDB,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    isVisibilityOff: Boolean,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invest.label ?: stringResource(id = R.string.no_label),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${stringResource(id = R.string.beginning)} : ${dateFormattedText(invest.dateBegin)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (invest.dateEnd != null && invest.dateEnd != 0.0) {
                        Text(
                            text = "${stringResource(id = R.string.end)} : ${dateFormattedText(invest.dateEnd)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                // Action to close or reopen an investment
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = if (invest.dateEnd == null || invest.dateEnd == 0.0) Icons.Default.Done else Icons.Default.Refresh,
                        contentDescription = stringResource(id = R.string.edit_closing_date),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    LabelValueSmall(label = stringResource(id = R.string.invested), amount = invest.invested ?: 0.0, isVisibilityOff = isVisibilityOff)
                    LabelValueSmall(label = stringResource(id = R.string.earned), amount = invest.earned ?: 0.0, isVisibilityOff = isVisibilityOff)
                }
                Column(horizontalAlignment = Alignment.End) {
                    val gain = (invest.earned ?: 0.0) - (invest.invested ?: 0.0)
                    LabelValueSmall(label = stringResource(id = R.string.capital_gain), amount = gain, isVisibilityOff = isVisibilityOff)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PercentageText(
                            amount = invest.annualProfitability ?: 0.0,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(" (${stringResource(id = R.string.annual)})", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        WheelDatePickerDialog(
            initialDate = if (invest.dateEnd != null && invest.dateEnd != 0.0) invest.dateEnd else ((System.currentTimeMillis() / 86400000.0) + 25569.0),
            onDismiss = { showDatePicker = false },
            onDateSelected = { date ->
                scope.launch {
                    investmentViewModel.updateInvestmentEndDate(invest.idInvest ?: "", date)
                    onRefresh()
                    showDatePicker = false
                }
            }
        )
    }
}

@Composable
fun LabelValue(label: String, amount: Double, isVisibilityOff: Boolean) {
    Column {
        Text(text = label, style = MaterialTheme.typography.bodySmall)
        CurrencyText(
            amount = amount,
            isVisibilityOff = isVisibilityOff,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LabelValueSmall(label: String, amount: Double, isVisibilityOff: Boolean) {
    Row {
        Text(text = "$label : ", style = MaterialTheme.typography.bodySmall)
        CurrencyText(
            amount = amount,
            isVisibilityOff = isVisibilityOff,
            style = MaterialTheme.typography.bodySmall
        )
    }
}
