package com.example.appfinancetest

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.HomeWork
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class HomeTimeRange(val labelResId: Int, val periodNameResId: Int, val days: Double) {
    ONE_MONTH(R.string.range_1m, R.string.period_1_month, 30.0),
    SIX_MONTHS(R.string.range_6m, R.string.period_6_months, 182.0),
    ONE_YEAR(R.string.range_1y, R.string.period_1_year, 365.0),
    FIVE_YEARS(R.string.range_5y, R.string.period_5_years, 1825.0),
    ALL(R.string.range_all, R.string.period_all, 36500.0)
}

data class PortfolioSlice(
    val name: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel
) {
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var windowHeightPx by remember { mutableFloatStateOf(0f) }
    var card2BottomInWindowPx by remember { mutableFloatStateOf(0f) }

    val dynamicPeekHeight = remember(windowHeightPx, card2BottomInWindowPx, density) {
        if (windowHeightPx > 0f && card2BottomInWindowPx > 0f) {
            val remainingPx = windowHeightPx - card2BottomInWindowPx
            with(density) {
                // Écart exact de 16.dp entre la carte 2 et le volet de transactions
                (remainingPx.toDp() - 16.dp).coerceAtLeast(76.dp)
            }
        } else {
            165.dp
        }
    }

    val sharedPreferences = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val userName = remember { sharedPreferences.getString("user_name", "Nicolas") ?: "Alex" }

    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    var isRangeLoaded by remember { mutableStateOf(false) }
    var selectedRange by remember { mutableStateOf(HomeTimeRange.ALL) }

    // Load saved time range preference ONCE at startup
    LaunchedEffect(Unit) {
        try {
            val savedRangeStr = prefs.homeTimeRangeFlow.first()
            if (savedRangeStr != null) {
                try {
                    selectedRange = HomeTimeRange.valueOf(savedRangeStr)
                } catch (e: Exception) {
                    selectedRange = HomeTimeRange.ALL
                }
            }
        } catch (e: Exception) {
            selectedRange = HomeTimeRange.ALL
        } finally {
            isRangeLoaded = true
        }
    }

    // Save selected time range when modified
    LaunchedEffect(selectedRange, isRangeLoaded) {
        if (isRangeLoaded) {
            prefs.saveHomeTimeRange(selectedRange.name)
        }
    }

    val netWorth by databaseViewModel.netWorth.observeAsState(null)
    val latestTransaction by databaseViewModel.latestTransaction.observeAsState(null)

    val oldestDate by produceState(initialValue = 25569.0, databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getFirstTransactionDate()
    }

    // Filters states
    var dateMinFilter by remember { mutableStateOf("") }
    var dateMaxFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var amountMinFilter by remember { mutableStateOf("") }
    var amountMaxFilter by remember { mutableStateOf("") }
    var showFilter by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(searchQuery) {
        labelFilter = searchQuery
    }

    val categoriesList by produceState(initialValue = emptyList(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getAllCategories()
    }
    val itemsList by produceState(initialValue = emptyList(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getAllItems()
    }
    val labelsList by produceState(initialValue = emptyList(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getAllLabels()
    }

    // Pagination for transactions list in bottom sheet
    val pageSize = 50
    val beforeRefresh = 15
    var currentPage by remember { mutableIntStateOf(1) }
    val transactionsPaged = remember { mutableStateListOf<TransactionDB>() }
    val listState = rememberLazyListState()
    var isFirstLoadPaged by remember { mutableStateOf(true) }

    LaunchedEffect(listState, dateMinFilter, dateMaxFilter, categoryFilter, itemFilter, labelFilter, amountMinFilter, amountMaxFilter, searchQuery) {
        val effectiveLabelFilter = if (searchQuery.isNotBlank()) searchQuery else labelFilter
        val isFilterActive = dateMinFilter.isNotBlank() || dateMaxFilter.isNotBlank() || categoryFilter.isNotBlank() || itemFilter.isNotBlank() || effectiveLabelFilter.isNotBlank() || amountMinFilter.isNotBlank() || amountMaxFilter.isNotBlank()

        if (isFilterActive) {
            val allTransactions = databaseViewModel.getTransactionsSortedByDateDESC()
            val filtered = filterTransactions(
                allTransactions,
                dateMinFilter,
                dateMaxFilter,
                categoryFilter,
                itemFilter,
                effectiveLabelFilter,
                amountMinFilter,
                amountMaxFilter
            )
            transactionsPaged.clear()
            transactionsPaged.addAll(filtered)
            isFirstLoadPaged = false
        } else {
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
    }

    LaunchedEffect(currentPage, refreshTrigger, dateMinFilter, dateMaxFilter, categoryFilter, itemFilter, labelFilter, amountMinFilter, amountMaxFilter, searchQuery) {
        val effectiveLabelFilter = if (searchQuery.isNotBlank()) searchQuery else labelFilter
        val isFilterActive = dateMinFilter.isNotBlank() || dateMaxFilter.isNotBlank() || categoryFilter.isNotBlank() || itemFilter.isNotBlank() || effectiveLabelFilter.isNotBlank() || amountMinFilter.isNotBlank() || amountMaxFilter.isNotBlank()

        if (!isFilterActive) {
            val offset = (currentPage - 1) * pageSize
            val newTransactions = databaseViewModel.getPagedTransactions(pageSize, offset)
            if (currentPage == 1) {
                transactionsPaged.clear()
            }
            transactionsPaged.addAll(newTransactions)
            isFirstLoadPaged = false
        }
    }

    val allInvestments by produceState<List<InvestmentDB>?>(initialValue = null, investmentViewModel, refreshTrigger) {
        value = investmentViewModel.getInvestment()
    }

    // Filter only ONGOING / ENGAGED investments (dateEnd is null or 0)
    val ongoingInvestments = remember(allInvestments) {
        allInvestments?.filter { it.dateEnd == null || it.dateEnd == 0.0 } ?: emptyList()
    }

    // Net Worth Sparkline & Variation States
    var sparklinePoints by remember { mutableStateOf<List<Float>>(emptyList()) }
    var rangeVariationAmount by remember { mutableStateOf<Double?>(null) }
    var rangeVariationPercent by remember { mutableStateOf<Double?>(null) }
    var hasValidDataForRange by remember { mutableStateOf(true) }

    var transactionToEdit by remember { mutableStateOf<TransactionDB?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Colors matching dark theme from the reference design
    val bgDark = Color(0xFF090E17)
    val cardBg = Color(0xFF111827)
    val cardBorder = Color(0xFF1E293B)
    val greenAccent = Color(0xFF00E676)
    val redAccent = Color(0xFFEF4444)
    val textPrimary = Color.White
    val textMuted = Color(0xFF94A3B8)
    val bluePill = Color(0xFF0284C7)

    // Calculate Sparkline Points and Evolution for Selected Time Range
    LaunchedEffect(netWorth, oldestDate, selectedRange, refreshTrigger) {
        withContext(Dispatchers.Default) {
            val today = (System.currentTimeMillis() / (1000 * 86400.0)) + 25569
            val currentNW = databaseViewModel.getNetWorthAtDateStatic(today)

            val targetStart = today - selectedRange.days

            if (selectedRange == HomeTimeRange.ALL) {
                hasValidDataForRange = false
                rangeVariationAmount = null
                rangeVariationPercent = null
            } else if (oldestDate > targetStart) {
                hasValidDataForRange = false
                rangeVariationAmount = null
                rangeVariationPercent = null
            } else {
                hasValidDataForRange = true
                val startExcel = targetStart.coerceAtLeast(oldestDate)
                val pastNW = databaseViewModel.getNetWorthAtDateStatic(startExcel)

                val diff = currentNW - pastNW
                val percent = if (pastNW != 0.0) (diff / abs(pastNW) * 100) else 0.0

                rangeVariationAmount = diff
                rangeVariationPercent = percent
            }

            // Sparkline chart points are always calculated and displayed (for ALL, 5A, and all ranges)
            val sparklineStart = if (selectedRange == HomeTimeRange.ALL) {
                oldestDate
            } else {
                targetStart.coerceAtLeast(oldestDate)
            }

            val samples = 15
            val step = (today - sparklineStart) / samples
            val points = mutableListOf<Float>()

            for (i in 0..samples) {
                val date = sparklineStart + (i * step)
                val nw = databaseViewModel.getNetWorthAtDateStatic(date)
                points.add(nw.toFloat())
            }
            sparklinePoints = points
        }
    }

    // Calculate Portfolio Breakdown based strictly on ONGOING investments (grouped by Objet / item) + Liquidités
    val portfolioSlices = remember(ongoingInvestments, netWorth) {
        val totalNW = (netWorth ?: 0.0).coerceAtLeast(0.0)

        val itemMap = mutableMapOf<String, Double>()
        ongoingInvestments.forEach { inv ->
            val itemKey = inv.item?.trim()?.ifBlank { "Autre" } ?: "Autre"
            val investedAmt = inv.invested ?: 0.0
            if (investedAmt > 0) {
                itemMap[itemKey] = (itemMap[itemKey] ?: 0.0) + investedAmt
            }
        }

        val totalOngoingInvested = itemMap.values.sum()
        if (totalNW > totalOngoingInvested) {
            val liquidites = totalNW - totalOngoingInvested
            if (liquidites > 0) {
                itemMap["Liquidités"] = (itemMap["Liquidités"] ?: 0.0) + liquidites
            }
        }

        val sliceTotal = if (totalNW > 0) totalNW else totalOngoingInvested.coerceAtLeast(1.0)

        val palette = listOf(
            Color(0xFF00E676), // Green
            Color(0xFF3B82F6), // Blue
            Color(0xFF8B5CF6), // Purple
            Color(0xFFF59E0B), // Amber
            Color(0xFFFACC15), // Yellow
            Color(0xFFA855F7), // Violet
            Color(0xFF06B6D4), // Cyan
            Color(0xFFEC4899), // Pink
            Color(0xFFF97316), // Orange
            Color(0xFF14B8A6)  // Teal
        )

        val itemColors = mapOf(
            "Immobilier" to Color(0xFF00E676),
            "Bourse - PEA" to Color(0xFF3B82F6),
            "Bourse" to Color(0xFF3B82F6),
            "Liquidités" to Color(0xFF8B5CF6),
            "Crypto" to Color(0xFFF59E0B),
            "Or" to Color(0xFFFACC15),
            "Crowdfunding" to Color(0xFFA855F7),
            "Crowdlending" to Color(0xFFA855F7),
            "Bourse - Compte titre" to Color(0xFF06B6D4)
        )
        var colorIdx = 0
        itemMap.map { (name, amount) ->
            val pct = ((amount / sliceTotal) * 100).toFloat()
            val col = itemColors[name] ?: palette[colorIdx % palette.size].also { colorIdx++ }
            PortfolioSlice(name, amount, pct, col)
        }.sortedByDescending { it.amount }
    }

    if (transactionToEdit != null) {
        TransactionEditDialog(
            transaction = transactionToEdit!!,
            onDismiss = { transactionToEdit = null },
            onSave = { updated ->
                scope.launch {
                    databaseViewModel.insertTransaction(updated)
                    calculateRunningBalance(databaseViewModel)
                    refreshTrigger++
                    transactionToEdit = null
                }
            }
        )
    }

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                val h = coords.size.height.toFloat()
                if (windowHeightPx != h) {
                    windowHeightPx = h
                }
            },
        scaffoldState = scaffoldState,
        sheetContainerColor = cardBg,
        sheetContentColor = Color.White,
        sheetPeekHeight = dynamicPeekHeight,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight(0.85f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.recent_movements),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textMuted,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isFilterActive = dateMinFilter.isNotBlank() || dateMaxFilter.isNotBlank() || categoryFilter.isNotBlank() || itemFilter.isNotBlank() || labelFilter.isNotBlank() || amountMinFilter.isNotBlank() || amountMaxFilter.isNotBlank() || searchQuery.isNotBlank()

                        if (isFilterActive) {
                            IconButton(onClick = {
                                dateMinFilter = ""
                                dateMaxFilter = ""
                                categoryFilter = ""
                                itemFilter = ""
                                labelFilter = ""
                                amountMinFilter = ""
                                amountMaxFilter = ""
                                searchQuery = ""
                                currentPage = 1
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Delete filters",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        IconButton(
                            onClick = { 
                                isSearching = !isSearching
                                if (!isSearching) {
                                    searchQuery = ""
                                    labelFilter = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSearching) Icons.Default.Clear else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = textMuted
                            )
                        }

                        IconButton(onClick = { showFilter = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = "Filter",
                                tint = if (isFilterActive) MaterialTheme.colorScheme.primary else textMuted
                            )
                        }
                    }
                }

                if (isSearching) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (isFirstLoadPaged) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(5) {
                            TransactionRowShimmer()
                            HorizontalDivider(thickness = 0.5.dp, color = cardBorder)
                        }
                    }
                } else if (transactionsPaged.isEmpty()) {
                    Text(
                        text = stringResource(id = R.string.no_transactions),
                        style = MaterialTheme.typography.bodyLarge,
                        color = textMuted,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(transactionsPaged) { item ->
                            HomeMovementItem(
                                item = item,
                                isVisibilityOff = isVisibilityOff,
                                onClick = {
                                    transactionToEdit = item
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                thickness = 0.5.dp,
                                color = cardBorder
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        },
        containerColor = bgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.hello) + " ",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Normal,
                                color = textPrimary
                            )
                        )
                        Text(
                            text = "$userName 👋",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(id = R.string.wealth_sentence),
                        style = MaterialTheme.typography.bodySmall,
                        color = textMuted
                    )
                }

                // Right Action Icons (Visibility Toggle + Settings Icon)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Visibility Toggle Icon (Eye / Crossed Eye)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, cardBorder, CircleShape)
                            .clickable {
                                scope.launch {
                                    prefs.saveVisibilityState(!isVisibilityOff)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVisibilityOff) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Settings Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, cardBorder, CircleShape)
                            .clickable {
                                showSettingsDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 1: NET WORTH + SPARKLINE + TIME RANGE PILLS
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
                        text = stringResource(id = R.string.net_worth_simple),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textMuted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Net Worth Amount
                            Text(
                                text = if (isVisibilityOff) "**** €" else formatCurrency(netWorth ?: 0.0),
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 28.sp,
                                    color = textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Variation amount & percentage lines
                            if (!hasValidDataForRange || rangeVariationAmount == null || rangeVariationPercent == null) {
                                Text(
                                    text = if (isVisibilityOff) "+*** €" else "N/A",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textMuted
                                    )
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (isVisibilityOff) "+*.*%" else "N/A",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = textMuted
                                    )
                                )
                            } else {
                                val isPos = (rangeVariationAmount ?: 0.0) >= 0
                                val varColor = if (isPos) greenAccent else redAccent
                                val sign = if (isPos) "+" else "-"
                                val absAmount = abs(rangeVariationAmount ?: 0.0)
                                val absPercent = abs(rangeVariationPercent ?: 0.0)

                                Text(
                                    text = if (isVisibilityOff) "+*** €" else "$sign${formatCurrency(absAmount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = varColor
                                    )
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (isVisibilityOff) "+*.*%" else "$sign${formatPercentage(absPercent)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = varColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = stringResource(id = selectedRange.periodNameResId),
                                style = MaterialTheme.typography.bodySmall,
                                color = textMuted
                            )
                        }

                        // Sparkline Canvas Chart on Right Side of Card
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(64.dp)
                        ) {
                            if (sparklinePoints.size >= 2) {
                                HomeSparklineChart(
                                    points = sparklinePoints,
                                    lineColor = greenAccent
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time Range Selector Pills (1M, 6M, 1A, 5A, ALL)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        HomeTimeRange.entries.forEach { range ->
                            val isSelected = range == selectedRange
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(17.dp))
                                    .background(if (isSelected) bluePill else Color(0xFF1E293B))
                                    .clickable { selectedRange = range },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = range.labelResId),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textMuted
                                    )
                                )
                            }
                        }
                    }

                    if (latestTransaction != null && latestTransaction?.balance != null) {
                        val lastBalance = latestTransaction!!.balance ?: 0.0
                        val lastDate = latestTransaction!!.date

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 14.dp),
                            thickness = 0.5.dp,
                            color = cardBorder
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.balance_as_of, dateFormattedText(lastDate)),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.bank_account),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = textMuted
                                    )
                                }
                            }

                            Text(
                                text = if (isVisibilityOff) "**** €" else formatCurrency(lastBalance),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = if (lastBalance >= 0) greenAccent else redAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CARD 2: RÉPARTITION DU PORTEFEUILLE (DONUT + LEGEND)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        val unscrolledBottom = coords.positionInRoot().y + coords.size.height + scrollState.value
                        if (card2BottomInWindowPx != unscrolledBottom) {
                            card2BottomInWindowPx = unscrolledBottom
                        }
                    },
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
                        text = stringResource(id = R.string.portfolio_breakdown),
                        style = MaterialTheme.typography.headlineSmall,
                        color = textMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Donut Chart Container on Left
                        Box(
                            modifier = Modifier.size(130.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            HomeDonutChart(slices = portfolioSlices)

                            // Net Worth Amount in Center
                            Text(
                                text = if (isVisibilityOff) "**** €" else formatCurrency(netWorth ?: 0.0),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = textPrimary
                                ),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Legend List on Right
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            portfolioSlices.forEach { slice ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(slice.color)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = slice.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Text(
                                        text = "%.1f%%".format(slice.percentage).replace('.', ','),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Espace identique (16.dp) entre la carte 2 et la liste de transactions
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSettingsDialog) {
        SettingsScreen(
            databaseViewModel = databaseViewModel,
            onDismiss = { showSettingsDialog = false }
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
            onClearAll = {
                dateMinFilter = ""
                dateMaxFilter = ""
                categoryFilter = ""
                itemFilter = ""
                labelFilter = ""
                amountMinFilter = ""
                amountMaxFilter = ""
                searchQuery = ""
                currentPage = 1
            },
            onDismiss = { showFilter = false }
        )
    }
}

@Composable
fun HomeSparklineChart(
    points: List<Float>,
    lineColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (points.size < 2) return@Canvas

        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val w = size.width
        val h = size.height
        val stepX = w / (points.size - 1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, pt ->
            val x = i * stepX
            val y = h - ((pt - minVal) / range * (h - 16f) + 8f)
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        // Soft gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.0f)
                )
            )
        )

        // Glowing stroke line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

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

@Composable
fun HomeMovementItem(
    item: TransactionDB,
    isVisibilityOff: Boolean,
    onClick: () -> Unit
) {
    val amount = item.amount ?: 0.0
    val isNegative = (item.variation ?: 0.0) < 0 || (item.category == "Charge" || item.category == "Investissement")
    val amountColor = if (isNegative) Color(0xFFEF4444) else Color(0xFF00E676)
    val sign = if (isNegative) "- " else "+ "

    val title = item.label ?: item.item ?: stringResource(id = R.string.purchase)
    val todayStr = stringResource(id = R.string.date_today)
    val yesterdayStr = stringResource(id = R.string.date_yesterday)
    val recentStr = stringResource(id = R.string.date_recent)
    val subtitle = formatMovementSubtitle(item, todayStr, yesterdayStr, recentStr)

    val (icon, iconBg) = remember(item) {
        getCategoryIconAndBg(item)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = if (isVisibilityOff) "**** €" else "$sign${formatCurrency(abs(amount))}",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
            color = amountColor
        )
    }
}

fun formatMovementSubtitle(
    item: TransactionDB,
    todayStr: String = "Aujourd'hui",
    yesterdayStr: String = "Hier",
    recentStr: String = "Récent"
): String {
    val dateText = if (item.date != null) {
        val excelMillis = ((item.date - 25569) * 86400 * 1000).toLong()
        val date = Date(excelMillis)
        val now = Calendar.getInstance()
        val transCal = Calendar.getInstance().apply { time = date }

        if (now.get(Calendar.YEAR) == transCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) == transCal.get(Calendar.DAY_OF_YEAR)
        ) {
            todayStr
        } else if (now.get(Calendar.YEAR) == transCal.get(Calendar.YEAR) &&
            now.get(Calendar.DAY_OF_YEAR) - transCal.get(Calendar.DAY_OF_YEAR) == 1
        ) {
            yesterdayStr
        } else {
            SimpleDateFormat("d MMM", Locale.getDefault()).format(date)
        }
    } else {
        recentStr
    }

    val elements = mutableListOf<String>()
    elements.add(dateText)
    if (!item.category.isNullOrBlank()) {
        elements.add(item.category)
    }
    if (!item.item.isNullOrBlank() && item.item != item.category) {
        elements.add(item.item)
    }
    if (elements.size == 1) {
        elements.add("PEA")
    }

    return elements.joinToString(" • ")
}

fun getCategoryIconAndBg(item: TransactionDB): Pair<ImageVector, Color> {
    val cat = (item.category ?: "") + " " + (item.item ?: "") + " " + (item.label ?: "")
    val catLower = cat.lowercase()

    return when {
        catLower.contains("btc") || catLower.contains("crypto") -> Pair(Icons.Default.CurrencyBitcoin, Color(0xFFF59E0B))
        catLower.contains(" bourse") || catLower.contains("etf") || catLower.contains("pea") || catLower.contains("action") -> Pair(Icons.Default.ShowChart, Color(0xFF1E293B))
        catLower.contains("salaire") || catLower.contains("virement") || catLower.contains("revenus") -> Pair(Icons.Default.ArrowDownward, Color(0xFF0D9488))
        catLower.contains("immobilier") || catLower.contains("loyer") -> Pair(Icons.Default.HomeWork, Color(0xFF10B981))
        else -> Pair(Icons.Default.AccountBalanceWallet, Color(0xFF334155))
    }
}
