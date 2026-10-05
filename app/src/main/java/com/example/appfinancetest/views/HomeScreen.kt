package com.example.appfinancetest.views

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.InvestmentDB
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.R
import com.example.appfinancetest.ui.theme.*
import com.example.appfinancetest.components.SearchField
import com.example.appfinancetest.components.TimeRangeSelectorPills
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.components.TransactionRowShimmer
import com.example.appfinancetest.components.saveTransactionAndSyncInvestments
import com.example.appfinancetest.calculations.dateFormattedText
import com.example.appfinancetest.calculations.filterTransactions
import com.example.appfinancetest.calculations.formatCurrency
import com.example.appfinancetest.calculations.formatPercentage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

import com.example.appfinancetest.classes.HomeTimeRange
import com.example.appfinancetest.classes.CreditDBViewModel
import com.example.appfinancetest.components.AppPieChart
import com.example.appfinancetest.components.PieChartLegendStyle
import com.example.appfinancetest.components.toPieChartSlice
import com.example.appfinancetest.components.HomeMovementItem
import com.example.appfinancetest.components.HomeSparklineChart

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
    investmentViewModel: InvestmentDBViewModel,
    creditViewModel: CreditDBViewModel
) {
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var showImportExport by remember { mutableStateOf(false) }

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

    val isDarkThemeCustom by prefs.isDarkThemeFlow.collectAsState(initial = null)
    val darkTheme = isDarkThemeCustom ?: isSystemInDarkTheme()

    // Colors supporting Light & Dark mode
    val bgDark = if (darkTheme) BgDark else MaterialTheme.colorScheme.background
    val cardBg = if (darkTheme) CardBg else MaterialTheme.colorScheme.surface
    val cardBorder = if (darkTheme) CardBorder else MaterialTheme.colorScheme.outlineVariant
    val greenAccent = GreenAccent
    val redAccent = RedAccent
    val textPrimary = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val textMuted = if (darkTheme) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val unselectedBg = if (darkTheme) UnselectedBg else MaterialTheme.colorScheme.surfaceVariant
    val bluePill = BluePill

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
            GreenAccent, // Green
            BourseBlue, // Blue
            LiquidityPurple, // Purple
            CryptoAmber, // Amber
            GoldYellow, // Yellow
            CrowdfundingViolet, // Violet
            Cyan500, // Cyan
            Pink, // Pink
            Orange, // Orange
            Teal  // Teal
        )

        val itemColors = mapOf(
            "Immobilier" to GreenAccent,
            "Bourse - PEA" to BourseBlue,
            "Bourse" to BourseBlue,
            "Liquidités" to LiquidityPurple,
            "Crypto" to CryptoAmber,
            "Or" to GoldYellow,
            "Crowdfunding" to CrowdfundingViolet,
            "Crowdlending" to CrowdfundingViolet,
            "Bourse - Compte titre" to Cyan500
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
                    val prevInvest = transactionToEdit?.idInvest
                    saveTransactionAndSyncInvestments(
                        updated = updated,
                        previousIdInvest = prevInvest,
                        databaseViewModel = databaseViewModel,
                        investmentViewModel = investmentViewModel
                    )
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
            .onGloballyPositioned { cords ->
                val h = cords.size.height.toFloat()
                if (windowHeightPx != h) {
                    windowHeightPx = h
                }
            },
        scaffoldState = scaffoldState,
        sheetContainerColor = cardBg,
        sheetContentColor = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface,
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
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Right Action Icons (Import/Export + Visibility Toggle + Settings Icon)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Import / Export Icon
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(cardBg)
                            .border(1.dp, cardBorder, CircleShape)
                            .clickable {
                                showImportExport = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_import_export),
                            contentDescription = "Import / Export",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

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
                        style = MaterialTheme.typography.headlineSmall
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
                                text = if (isVisibilityOff) "**** €" else formatCurrency(
                                    netWorth ?: 0.0
                                ),
                                style = MaterialTheme.typography.headlineMedium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Variation amount & percentage lines
                            if (!hasValidDataForRange || rangeVariationAmount == null || rangeVariationPercent == null) {
                                Text(
                                    text = if (isVisibilityOff) "+*** €" else "N/A",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "N/A",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            } else {
                                val isPos = (rangeVariationAmount ?: 0.0) >= 0
                                val varColor = if (isPos) greenAccent else redAccent
                                val sign = if (isPos) "+" else "-"
                                val absAmount = abs(rangeVariationAmount ?: 0.0)
                                val absPercent = abs(rangeVariationPercent ?: 0.0)

                                Text(
                                    text = if (isVisibilityOff) "+*** €" else "$sign${
                                        formatCurrency(
                                            absAmount
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = varColor
                                    )
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "$sign${
                                        formatPercentage(
                                            absPercent
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = varColor
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = stringResource(id = selectedRange.periodNameResId),
                                style = MaterialTheme.typography.bodySmall
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
                    TimeRangeSelectorPills(
                        selectedRange = selectedRange,
                        onRangeSelected = { selectedRange = it },
                        bluePill = bluePill,
                        unselectedBg = unselectedBg,
                        textMuted = textMuted,
                        textStyle = MaterialTheme.typography.bodyMedium
                    )

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
                                        .background(if (darkTheme) CardBorder else MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = if (darkTheme) Color.White else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = stringResource(id = R.string.balance_as_of,
                                            dateFormattedText(lastDate)
                                        ),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(id = R.string.bank_account),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            Text(
                                text = if (isVisibilityOff) "**** €" else formatCurrency(lastBalance),
                                style = MaterialTheme.typography.headlineMedium,
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
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    AppPieChart(
                        slices = portfolioSlices.map { it.toPieChartSlice() },
                        isClickable = false,
                        centerText = if (isVisibilityOff) "**** €" else formatCurrency(netWorth ?: 0.0),
                        holeRadiusRatio = 68f,
                        chartHeight = 130.dp,
                        legendStyle = PieChartLegendStyle.COMPACT_RIGHT,
                        isVisibilityOff = isVisibilityOff
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSettingsDialog) {
        SettingsScreen(
            databaseViewModel = databaseViewModel,
            onDismiss = { showSettingsDialog = false }
        )
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