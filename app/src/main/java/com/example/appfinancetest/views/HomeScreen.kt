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
import com.example.appfinancetest.R
import com.example.appfinancetest.calculations.dateFormattedText
import com.example.appfinancetest.calculations.formatCurrency
import com.example.appfinancetest.calculations.formatPercentage
import com.example.appfinancetest.classes.CreditDBViewModel
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.HomeTimeRange
import com.example.appfinancetest.classes.InvestmentDB
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.components.AppPieChart
import com.example.appfinancetest.components.HomeSparklineChart
import com.example.appfinancetest.components.PieChartLegendStyle
import com.example.appfinancetest.components.TimeRangeSelectorPills
import com.example.appfinancetest.components.TransactionsBottomSheetScaffold
import com.example.appfinancetest.components.TransactionsSheetContent
import com.example.appfinancetest.components.toPieChartSlice
import com.example.appfinancetest.components.TopBar
import com.example.appfinancetest.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

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

    TransactionsBottomSheetScaffold(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { cords ->
                val h = cords.size.height.toFloat()
                if (windowHeightPx != h) {
                    windowHeightPx = h
                }
            },
        databaseViewModel = databaseViewModel,
        investmentViewModel = investmentViewModel,
        isVisibilityOff = isVisibilityOff,
        refreshTrigger = refreshTrigger,
        onRefreshNeeded = {
            refreshTrigger++
            databaseViewModel.refreshNetWorth()
        },
        sheetPeekHeight = dynamicPeekHeight
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
            TopBar(
                titleContent = {
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
                },
                onImportExportClick = { showImportExport = true },
                onVisibilityClick = {
                    scope.launch {
                        prefs.saveVisibilityState(!isVisibilityOff)
                    }
                },
                isVisibilityOff = isVisibilityOff,
                onSettingsClick = { showSettingsDialog = true },
                cardBg = cardBg,
                cardBorder = cardBorder,
                textPrimary = textPrimary
            )

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
}