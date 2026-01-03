package com.example.appfinancetest

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    creditViewModel: CreditDBViewModel
)  {

    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val transactions by produceState(initialValue = emptyList(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    val netWorth by databaseViewModel.netWorth.observeAsState(0.0)

    var showValidation by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    var isFiltersLoaded by remember { mutableStateOf(false) }

    // Filters states
    var dateMinFilter by remember { mutableStateOf("") }
    var dateMaxFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var amountMinFilter by remember { mutableStateOf("") }
    var amountMaxFilter by remember { mutableStateOf("") }

    // Load filters ONCE at startup
    LaunchedEffect(Unit) {
        try {
            dateMinFilter = prefs.dashboardDateMinFilterFlow.first() ?: ""
            dateMaxFilter = prefs.dashboardDateMaxFilterFlow.first() ?: ""
            categoryFilter = prefs.dashboardCategoryFilterFlow.first() ?: ""
            itemFilter = prefs.dashboardItemFilterFlow.first() ?: ""
            labelFilter = prefs.dashboardLabelFilterFlow.first() ?: ""
            amountMinFilter = prefs.dashboardAmountMinFilterFlow.first() ?: ""
            amountMaxFilter = prefs.dashboardAmountMaxFilterFlow.first() ?: ""
        } catch (e: Exception) { }
        isFiltersLoaded = true
    }

    // Save filters when they change
    LaunchedEffect(dateMinFilter, dateMaxFilter, categoryFilter, itemFilter, labelFilter, amountMinFilter, amountMaxFilter, isFiltersLoaded) {
        if (isFiltersLoaded) {
            prefs.saveDashboardDateMinFilter(dateMinFilter)
            prefs.saveDashboardDateMaxFilter(dateMaxFilter)
            prefs.saveDashboardCategoryFilter(categoryFilter)
            prefs.saveDashboardItemFilter(itemFilter)
            prefs.saveDashboardLabelFilter(labelFilter)
            prefs.saveDashboardAmountMinFilter(amountMinFilter)
            prefs.saveDashboardAmountMaxFilter(amountMaxFilter)
        }
    }

    // Pagination for transactions list
    val pageSize = 50
    val beforeRefresh = 15
    var currentPage by remember { mutableIntStateOf(1) }
    val transactionsPaged = remember { mutableStateListOf<TransactionDB>() }
    val listState = rememberLazyListState()
    var isFirstLoadPaged by remember { mutableStateOf(true) }

    val clearFilters = {
        dateMinFilter = ""
        dateMaxFilter = ""
        categoryFilter = ""
        itemFilter = ""
        labelFilter = ""
        amountMinFilter = ""
        amountMaxFilter = ""
        currentPage = 1
    }

    // Dynamic dropdown options based on current selections
    val categoriesList = remember(transactions) {
        transactions.mapNotNull { it.category }.distinct().filter { it.isNotBlank() }.sorted()
    }
    
    val itemsList = remember(transactions, categoryFilter) {
        transactions.filter { categoryFilter.isBlank() || it.category?.contains(categoryFilter, ignoreCase = true) == true }
            .mapNotNull { it.item }.distinct().filter { it.isNotBlank() }.sorted()
    }
    
    val labelsList = remember(transactions, categoryFilter, itemFilter) {
        transactions.filter {
            (categoryFilter.isBlank() || it.category?.contains(categoryFilter, ignoreCase = true) == true) &&
            (itemFilter.isBlank() || it.item?.contains(itemFilter, ignoreCase = true) == true)
        }.mapNotNull { it.label }.distinct().filter { it.isNotBlank() }.sorted()
    }

    LaunchedEffect(listState) {
        delay(200)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleItemIndex ->
            if (lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= transactionsPaged.size - beforeRefresh &&
                transactionsPaged.size >= pageSize * (currentPage - 1)
            ) {
                currentPage += 1
            }
        }
    }

    LaunchedEffect(currentPage, refreshTrigger, dateMinFilter, dateMaxFilter, categoryFilter, itemFilter, labelFilter, amountMinFilter, amountMaxFilter, isFiltersLoaded) {
        if (!isFiltersLoaded) return@LaunchedEffect
        
        scope.launch {
            val isFilterActive = dateMinFilter.isNotBlank() || dateMaxFilter.isNotBlank() || categoryFilter.isNotBlank() || itemFilter.isNotBlank() || labelFilter.isNotBlank() || amountMinFilter.isNotBlank() || amountMaxFilter.isNotBlank()
            
            if (isFilterActive) {
                val allTransactions = databaseViewModel.getTransactionsSortedByDateDESC()
                val filtered = filterTransactions(allTransactions, dateMinFilter, dateMaxFilter, categoryFilter, itemFilter, labelFilter, amountMinFilter, amountMaxFilter)
                transactionsPaged.clear()
                transactionsPaged.addAll(filtered)
            } else {
                val offset = (currentPage - 1) * pageSize
                val newTransactions = databaseViewModel.getPagedTransactions(pageSize, offset)
                if (currentPage == 1) {
                    transactionsPaged.clear()
                    isFirstLoadPaged = false
                }
                transactionsPaged.addAll(newTransactions)
            }
        }
    }

    if (showValidation) {
        InvestmentValidationInterface(databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel, onDismiss = { showValidation = false }, onRefresh = { refreshTrigger++ })
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
                currentPage = 1
                isFirstLoadPaged = true
            }
        )
    }
    if (showFilter) {
        TransactionFilterInterface(
            dateMinFilter = dateMinFilter,
            onDateMinFilterChange = { dateMinFilter = it; currentPage = 1 },
            dateMaxFilter = dateMaxFilter,
            onDateMaxFilterChange = { dateMaxFilter = it; currentPage = 1 },
            categoryFilter = categoryFilter,
            onCategoryFilterChange = { categoryFilter = it; currentPage = 1 },
            categories = categoriesList,
            itemFilter = itemFilter,
            onItemFilterChange = { itemFilter = it; currentPage = 1 },
            items = itemsList,
            labelFilter = labelFilter,
            onLabelFilterChange = { labelFilter = it; currentPage = 1 },
            labels = labelsList,
            amountMinFilter = amountMinFilter,
            onAmountMinFilterChange = { amountMinFilter = it; currentPage = 1 },
            amountMaxFilter = amountMaxFilter,
            onAmountMaxFilterChange = { amountMaxFilter = it; currentPage = 1 },
            onClearAll = clearFilters,
            onDismiss = { showFilter = false }
        )
    }

    // Evolutions states
    var evo6m by remember { mutableStateOf<Double?>(null) }
    var evo1y by remember { mutableStateOf<Double?>(null) }
    var evo5y by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(netWorth, transactions) {
        if (transactions.isNotEmpty()) {
            val today = (System.currentTimeMillis() / (1000 * 86400.0)) + 25569
            val currentNW = databaseViewModel.getNetWorthAtDateStatic(today)
            
            val nw6m = databaseViewModel.getNetWorthAtDateStatic(today - 182)
            val nw1y = databaseViewModel.getNetWorthAtDateStatic(today - 365)
            val nw5y = databaseViewModel.getNetWorthAtDateStatic(today - 1825)

            evo6m = if (nw6m != 0.0) ((currentNW - nw6m) / nw6m * 100) else null
            evo1y = if (nw1y != 0.0) ((currentNW - nw1y) / nw1y * 100) else null
            evo5y = if (nw5y != 0.0) ((currentNW - nw5y) / nw5y * 100) else null
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                onValidateClick = { showValidation = true },
                onSettingsClick = { showSettings = true },
                onImportExportClick = { showImportExport = true },
                name = stringResource(id = R.string.dashboard_title)
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { _ ->
                            // Interaction standard
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.net_worth),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${"%.2f".format(netWorth ?: 0.0)} €",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = if ((netWorth ?: 0.0) >= 0) Color.Green else Color.Red,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.Center
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        EvolutionItem("6M", evo6m)
                        Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                        EvolutionItem("1Y", evo1y)
                        Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                        EvolutionItem("5Y", evo5y)
                    }

                    if (transactions.isNotEmpty()) {
                        val lastTransaction = transactions.lastOrNull()
                        val lastBalance = lastTransaction?.balance ?: 0.0
                        val lastDate = lastTransaction?.date ?: 0.0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.balance_as_of, dateFormattedText(lastDate)),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${"%.2f".format(lastBalance)} €",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.outlineVariant)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.recent_transactions),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        val isFilterActive = dateMinFilter.isNotBlank() || dateMaxFilter.isNotBlank() || categoryFilter.isNotBlank() || itemFilter.isNotBlank() || labelFilter.isNotBlank() || amountMinFilter.isNotBlank() || amountMaxFilter.isNotBlank()
                        
                        if (isFilterActive) {
                            IconButton(onClick = clearFilters) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Delete filters",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        IconButton(onClick = { showFilter = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Filter",
                                tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (transactions.isEmpty() && isFiltersLoaded && !isFirstLoadPaged) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(top = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    ) {
                        Text(
                            stringResource(id = R.string.no_transactions),
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        ImportActionCard(
                            databaseViewModel = databaseViewModel,
                            investmentViewModel = investmentViewModel,
                            creditViewModel = creditViewModel,
                            onRefresh = {
                                refreshTrigger++
                                currentPage = 1
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transactionsPaged) { transaction ->
                            TransactionRow(transaction)
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun EvolutionItem(label: String, evolution: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(
            text = if (evolution == null) "N/A" else (if (evolution >= 0) "+" else "") + "%.1f%%".format(evolution),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = when {
                evolution == null -> MaterialTheme.colorScheme.onSurface
                evolution >= 0 -> Color.Green
                else -> Color.Red
            }
        )
    }
}

@Composable
fun TransactionRow(transaction: TransactionDB) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = transaction.label ?: stringResource(id = R.string.no_label),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = "${dateFormattedText(transaction.date)} • ${transaction.category} • ${transaction.item}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val isNegative =
            (transaction.category == "Charge") || transaction.category == "Investissement"
        Text(
            text = "${if (isNegative) "-" else "+"}${"%.2f".format(transaction.amount ?: 0.0)} €",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (isNegative) Color.Red else Color.Green,
            textAlign = TextAlign.End
        )
    }
}
