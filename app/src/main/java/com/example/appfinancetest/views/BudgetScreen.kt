package com.example.appfinancetest.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.R
import com.example.appfinancetest.ui.theme.*
import com.example.appfinancetest.classes.CreditDBViewModel
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.components.PatrimonialLineChart
import com.example.appfinancetest.components.ClusteredColumnChartCard
import com.example.appfinancetest.components.BalancePieChart
import com.example.appfinancetest.components.TimeRangeSelectorPills
import com.example.appfinancetest.classes.HomeTimeRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class BudgetPeriodMode {
    MONTHLY,
    YEARLY
}

data class FlowBarData(
    val id: String,
    val label: String,
    val year: Int,
    val month: Int,
    val inflows: Double,
    val outflows: Double
)

/**
 * Pre-indexes transactions for ultra-fast inflow/outflow calculations across periods.
 */
class PreparedTransactions(val allTransactions: List<TransactionDB>) {
    private val investmentKeyMap: Map<String, List<TransactionDB>>

    init {
        val keys = mutableMapOf<String, MutableList<TransactionDB>>()
        allTransactions.forEach { it ->
            if (it.date != null && (it.category == "Investissement" || it.category == "Gain investissement")) {
                val key = it.idInvest?.ifBlank { null } ?: it.item?.ifBlank { null }
                if (key != null) {
                    keys.getOrPut(key) { mutableListOf() }.add(it)
                }
            }
        }
        investmentKeyMap = keys
    }

    fun calculateInflowsAndOutflows(
        periodStartExcel: Double,
        periodEndExcel: Double
    ): Pair<Double, Double> {
        val periodTx = allTransactions.filter { it ->
            it.date != null && it.date >= periodStartExcel && it.date <= periodEndExcel
        }

        // 1. Standard Revenues
        val standardRevenues = periodTx.filter { it.category == "Revenus" }
            .sumOf { it.amount ?: 0.0 }

        // 2. Investment Gains exceeding cumulative invested amount
        var investmentGainRevenues = 0.0

        for ((_, keyTx) in investmentKeyMap) {
            val cumulativeInvested = keyTx.filter {
                it.category == "Investissement" && (it.date ?: 0.0) <= periodEndExcel
            }.sumOf { it.amount ?: 0.0 }

            val cumulativeGainsBefore = keyTx.filter {
                it.category == "Gain investissement" && (it.date ?: 0.0) < periodStartExcel
            }.sumOf { it.amount ?: 0.0 }

            val gainsInPeriod = keyTx.filter {
                it.category == "Gain investissement" && (it.date ?: 0.0) >= periodStartExcel && (it.date ?: 0.0) <= periodEndExcel
            }.sumOf { it.amount ?: 0.0 }

            if (gainsInPeriod > 0.0) {
                val cumulativeGainsEnd = cumulativeGainsBefore + gainsInPeriod
                val profitPortion = (cumulativeGainsEnd - cumulativeInvested).coerceAtLeast(0.0)
                val taxableGainInPeriod = minOf(gainsInPeriod, profitPortion)
                investmentGainRevenues += taxableGainInPeriod
            }
        }

        val totalInflows = standardRevenues + investmentGainRevenues

        // 3. Outflows / Charges (ignoring Investissement)
        val totalOutflows = periodTx.filter {
            it.category != "Revenus" &&
            it.category != "Investissement" &&
            it.category != "Gain investissement"
        }.sumOf { abs(it.amount ?: 0.0) }

        return Pair(totalInflows, totalOutflows)
    }
}

@Composable
fun BudgetScreen(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    creditViewModel: CreditDBViewModel
) {
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var hideMarkerTrigger by remember { mutableIntStateOf(0) }

    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)
    val isDarkThemeCustom by prefs.isDarkThemeFlow.collectAsState(initial = null)
    val darkTheme = isDarkThemeCustom ?: isSystemInDarkTheme()

    // Dynamic colors supporting Light & Dark mode
    val bgDark = if (darkTheme) BgDark else MaterialTheme.colorScheme.background
    val cardBg = if (darkTheme) CardBg else MaterialTheme.colorScheme.surface
    val cardBorder = if (darkTheme) CardBorder else MaterialTheme.colorScheme.outlineVariant
    val textPrimary = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val textMuted = if (darkTheme) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val unselectedBg = if (darkTheme) UnselectedBg else MaterialTheme.colorScheme.surfaceVariant
    val bluePill = BluePill

    val allTransactions by produceState(initialValue = emptyList(), databaseViewModel, refreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    val preparedTx = remember(allTransactions) {
        PreparedTransactions(allTransactions)
    }

    // Dynamic extraction of categories from database
    val databaseCategories = remember(allTransactions) {
        allTransactions.mapNotNull { it.category }.filter { it.isNotBlank() }.distinct().sorted()
    }

    val overviewTitle = stringResource(id = R.string.overview_title)
    val viewChips = remember(overviewTitle, databaseCategories) {
        listOf(overviewTitle) + databaseCategories
    }

    var selectedViewChip by remember { mutableStateOf(overviewTitle) }

    // Period Navigator state (Card 1)
    var periodMode by remember { mutableStateOf(BudgetPeriodMode.MONTHLY) }
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }

    // Calculate year range for continuous scrolling across all transactions
    val (minYear, maxYear) = remember(allTransactions) {
        val currentY = Calendar.getInstance().get(Calendar.YEAR)
        if (allTransactions.isEmpty()) {
            Pair(currentY - 3, currentY + 1)
        } else {
            val validDates = allTransactions.mapNotNull { it.date }
            if (validDates.isEmpty()) {
                Pair(currentY - 3, currentY + 1)
            } else {
                val years = validDates.map { date ->
                    val millis = ((date - 25569.0) * 86400.0 * 1000.0).toLong()
                    val cal = Calendar.getInstance().apply { timeInMillis = millis }
                    cal.get(Calendar.YEAR)
                }
                val minY = (years.minOrNull() ?: currentY).coerceAtMost(currentY - 2)
                val maxY = (years.maxOrNull() ?: currentY).coerceAtLeast(currentY + 1)
                Pair(minY, maxY)
            }
        }
    }

    // Initialize period state to the date of the latest transaction if available
    LaunchedEffect(allTransactions) {
        if (allTransactions.isNotEmpty()) {
            val validDates = allTransactions.mapNotNull { it.date }
            if (validDates.isNotEmpty()) {
                val maxDate = validDates.maxOrNull() ?: 0.0
                val millis = ((maxDate - 25569.0) * 86400.0 * 1000.0).toLong()
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                selectedYear = cal.get(Calendar.YEAR)
                selectedMonth = cal.get(Calendar.MONTH)
            }
        }
    }

    // Line Chart Range state (Card 4)
    var isRangeLoaded by remember { mutableStateOf(false) }
    var selectedRange by remember { mutableStateOf(HomeTimeRange.ALL) }

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

    LaunchedEffect(selectedRange, isRangeLoaded) {
        if (isRangeLoaded) {
            prefs.saveHomeTimeRange(selectedRange.name)
        }
    }

    val scrollState = rememberScrollState()

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.budget_title),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = textPrimary
                    )
                )

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
                            .clickable { showImportExport = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_import_export),
                            contentDescription = "Import / Export",
                            tint = textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Visibility Toggle Icon
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
                            .clickable { showSettings = true },
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

            // VIEW CHIPS ("Bulles" selector)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(viewChips) { chipTitle ->
                    val isSelected = selectedViewChip == chipTitle
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isSelected) bluePill else cardBg)
                            .border(
                                BorderStroke(1.dp, if (isSelected) bluePill else cardBorder),
                                RoundedCornerShape(22.dp)
                            )
                            .clickable { selectedViewChip = chipTitle }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = chipTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) Color.White else textMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // CONTENT BASED ON SELECTED CHIP
            if (selectedViewChip != overviewTitle) {
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
                        // Time Range Selector Pills (1M, 6M, 1A, 5A, TOUT)
                        TimeRangeSelectorPills(
                            selectedRange = selectedRange,
                            onRangeSelected = { selectedRange = it },
                            bluePill = bluePill,
                            unselectedBg = unselectedBg,
                            textMuted = textMuted
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val todayExcel = remember { (System.currentTimeMillis() / (1000 * 86400.0)) + 25569 }
                        val categoryChartStartExcel = remember(selectedRange) {
                            if (selectedRange == HomeTimeRange.ALL) 0.0
                            else todayExcel - selectedRange.days
                        }

                        BalancePieChart(
                            viewModel = databaseViewModel,
                            startDate = categoryChartStartExcel,
                            endDate = todayExcel,
                            initialCategory = selectedViewChip
                        )
                    }
                }
            } else {
                // OVERVIEW VIEW

                // CARD 1: PERIOD NAVIGATOR
                val currentSelectedId = remember(periodMode, selectedYear, selectedMonth) {
                    if (periodMode == BudgetPeriodMode.MONTHLY) "$selectedYear-$selectedMonth" else "$selectedYear"
                }

                // CARD 3: Earn VS Spend (Clustered Column Chart) - defined early to compute chartBarData and navigation bounds
                val currentLocale = LocalConfiguration.current.locales[0]
                val chartBarData = remember(preparedTx, periodMode, minYear, maxYear, currentLocale) {
                    val barList = mutableListOf<FlowBarData>()
                    if (periodMode == BudgetPeriodMode.MONTHLY) {
                        val sdf = SimpleDateFormat("MMM", currentLocale)
                        for (yVal in minYear..maxYear) {
                            for (mIndex in 0..11) {
                                val startCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, yVal)
                                    set(Calendar.MONTH, mIndex)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                val endCal = (startCal.clone() as Calendar).apply {
                                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, 23)
                                    set(Calendar.MINUTE, 59)
                                    set(Calendar.SECOND, 59)
                                }
                                val start = (startCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0
                                val end = (endCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0

                                val periodTx = preparedTx.allTransactions.filter { it.date != null && it.date >= start && it.date <= end }
                                if (periodTx.isNotEmpty()) {
                                    val (inflows, outflows) = preparedTx.calculateInflowsAndOutflows(start, end)

                                    val shortYear = yVal.toString().takeLast(2)
                                    val mName = sdf.format(startCal.time).replaceFirstChar {
                                        if (it.isLowerCase()) it.titlecase(currentLocale) else it.toString()
                                    }
                                    val labelText = "$mName '$shortYear"

                                    barList.add(
                                        FlowBarData(
                                            id = "$yVal-$mIndex",
                                            label = labelText,
                                            year = yVal,
                                            month = mIndex,
                                            inflows = inflows,
                                            outflows = outflows
                                        )
                                    )
                                }
                            }
                        }
                    } else {
                        for (yVal in minYear..maxYear) {
                            val startCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, yVal)
                                set(Calendar.MONTH, 0)
                                set(Calendar.DAY_OF_MONTH, 1)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val endCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, yVal)
                                set(Calendar.MONTH, 11)
                                set(Calendar.DAY_OF_MONTH, 31)
                                set(Calendar.HOUR_OF_DAY, 23)
                                set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59)
                            }
                            val start = (startCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0
                            val end = (endCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0

                            val periodTx = preparedTx.allTransactions.filter { it.date != null && it.date >= start && it.date <= end }
                            if (periodTx.isNotEmpty()) {
                                val (inflows, outflows) = preparedTx.calculateInflowsAndOutflows(start, end)

                                barList.add(
                                    FlowBarData(
                                        id = "$yVal",
                                        label = "$yVal",
                                        year = yVal,
                                        month = 0,
                                        inflows = inflows,
                                        outflows = outflows
                                    )
                                )
                            }
                        }
                    }
                    barList
                }

                val currentIndex = remember(chartBarData, currentSelectedId) {
                    chartBarData.indexOfFirst { it.id == currentSelectedId }
                }
                val hasPrevious = currentIndex > 0
                val hasNext = currentIndex >= 0 && currentIndex < chartBarData.size - 1

                LaunchedEffect(chartBarData, periodMode) {
                    if (chartBarData.isNotEmpty()) {
                        val currentId = if (periodMode == BudgetPeriodMode.MONTHLY) "$selectedYear-$selectedMonth" else "$selectedYear"
                        if (chartBarData.none { it.id == currentId }) {
                            val last = chartBarData.last()
                            selectedYear = last.year
                            selectedMonth = last.month
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Navigation arrows + period text
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        val prev = chartBarData[currentIndex - 1]
                                        selectedYear = prev.year
                                        selectedMonth = prev.month
                                    }
                                },
                                enabled = hasPrevious,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Period",
                                    tint = if (hasPrevious) textPrimary else textMuted
                                )
                            }

                            val periodText = remember(periodMode, selectedYear, selectedMonth, chartBarData) {
                                if (chartBarData.isEmpty()) {
                                    "Aucune transaction"
                                } else if (periodMode == BudgetPeriodMode.MONTHLY) {
                                    val cal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, selectedYear)
                                        set(Calendar.MONTH, selectedMonth)
                                        set(Calendar.DAY_OF_MONTH, 1)
                                    }
                                    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                    val formatted = sdf.format(cal.time)
                                    formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                                } else {
                                    selectedYear.toString()
                                }
                            }

                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            IconButton(
                                onClick = {
                                    if (currentIndex >= 0 && currentIndex < chartBarData.size - 1) {
                                        val next = chartBarData[currentIndex + 1]
                                        selectedYear = next.year
                                        selectedMonth = next.month
                                    }
                                },
                                enabled = hasNext,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Period",
                                    tint = if (hasNext) textPrimary else textMuted
                                )
                            }
                        }

                        // Right: Mode selector (Monthly / Annual)
                        Row(
                            modifier = Modifier
                                .height(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(bgDark)
                                .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(18.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (periodMode == BudgetPeriodMode.MONTHLY) bluePill else Color.Transparent)
                                    .clickable { periodMode = BudgetPeriodMode.MONTHLY }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.period_monthly),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (periodMode == BudgetPeriodMode.MONTHLY) FontWeight.Bold else FontWeight.Normal,
                                        color = if (periodMode == BudgetPeriodMode.MONTHLY) Color.White else textMuted
                                    ),
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (periodMode == BudgetPeriodMode.YEARLY) bluePill else Color.Transparent)
                                    .clickable { periodMode = BudgetPeriodMode.YEARLY }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(id = R.string.period_yearly),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = if (periodMode == BudgetPeriodMode.YEARLY) FontWeight.Bold else FontWeight.Normal,
                                        color = if (periodMode == BudgetPeriodMode.YEARLY) Color.White else textMuted
                                    ),
                                )
                            }
                        }
                    }
                }

                // CARD 2: SAVINGS RATE
                val (startExcel, endExcel) = remember(periodMode, selectedYear, selectedMonth) {
                    if (periodMode == BudgetPeriodMode.MONTHLY) {
                        val startCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, selectedMonth)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endCal = (startCal.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }
                        val start = (startCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0
                        val end = (endCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0
                        Pair(start, end)
                    } else {
                        val startCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, 0)
                            set(Calendar.DAY_OF_MONTH, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val endCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, selectedYear)
                            set(Calendar.MONTH, 11)
                            set(Calendar.DAY_OF_MONTH, 31)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }
                        val start = (startCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0
                        val end = (endCal.timeInMillis / (1000.0 * 86400.0)) + 25569.0
                        Pair(start, end)
                    }
                }

                val (totalInflows, totalOutflows) = remember(preparedTx, startExcel, endExcel) {
                    preparedTx.calculateInflowsAndOutflows(startExcel, endExcel)
                }

                val netSavings = totalInflows - totalOutflows
                val savingsRate = if (totalInflows > 0) ((netSavings / totalInflows) * 100.0) else 0.0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, cardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Saving rate text
                        Text(
                            text = stringResource(id = R.string.savings_rate_label),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        // Ring Gauge Progress
                        Box(
                            modifier = Modifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val progress = (savingsRate.coerceIn(0.0, 100.0) / 100.0).toFloat()
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxSize(),
                                color = GreenAccent,
                                trackColor = CardBorder,
                                strokeWidth = 6.dp,
                                strokeCap = StrokeCap.Round
                            )
                        }

                        // Percentage text
                        val formattedRate = remember(savingsRate, isVisibilityOff) {
                            if (isVisibilityOff) {
                                "••,• %"
                            } else {
                                String.format(Locale.FRENCH, "%.1f %%", savingsRate)
                            }
                        }

                        Text(
                            text = formattedRate,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }

                // CARD 3: Earn VS Spend (Clustered Column Chart)
                ClusteredColumnChartCard(
                    title = stringResource(id = R.string.inflows_vs_outflows),
                    barDataList = chartBarData,
                    selectedId = currentSelectedId,
                    isVisibilityOff = isVisibilityOff,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMuted = textMuted,
                    textPrimary = textPrimary,
                    onPeriodClick = { clickedData ->
                        selectedYear = clickedData.year
                        if (periodMode == BudgetPeriodMode.MONTHLY) {
                            selectedMonth = clickedData.month
                        }
                    }
                )

                // CARD 4: ÉVOLUTION DU PATRIMOINE (Line Chart with @PatrimonialLineChart.kt)
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
                            text = stringResource(id = R.string.estate_evolution),
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Time Range Selector Pills (1M, 6M, 1A, 5A, TOUT)
                        TimeRangeSelectorPills(
                            selectedRange = selectedRange,
                            onRangeSelected = { selectedRange = it },
                            bluePill = bluePill,
                            unselectedBg = unselectedBg,
                            textMuted = textMuted
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val todayExcel = remember { (System.currentTimeMillis() / (1000 * 86400.0)) + 25569 }
                        val chartStartExcel = remember(selectedRange) {
                            if (selectedRange == HomeTimeRange.ALL) 0.0
                            else todayExcel - selectedRange.days
                        }

                        PatrimonialLineChart(
                            viewModel = databaseViewModel,
                            investmentViewModel = investmentViewModel,
                            startDate = chartStartExcel,
                            endDate = todayExcel,
                            refreshTrigger = refreshTrigger,
                            hideMarkerTrigger = hideMarkerTrigger,
                            onHideMarkers = { hideMarkerTrigger++ }
                        )
                    }
                }
            }
        }
    }
}

