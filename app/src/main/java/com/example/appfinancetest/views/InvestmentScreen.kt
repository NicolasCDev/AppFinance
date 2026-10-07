package com.example.appfinancetest.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.R
import com.example.appfinancetest.ui.theme.*
import com.example.appfinancetest.classes.CreditDBViewModel
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.InvestmentDB
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.classes.TransactionDB
import java.util.Locale
import com.example.appfinancetest.components.ImportActionCard
import com.example.appfinancetest.components.InvestmentCategoryCardShimmer
import com.example.appfinancetest.components.InvestmentHeatmapView
import com.example.appfinancetest.components.InvestmentListView
import com.example.appfinancetest.components.InvestmentSummaryCard
import com.example.appfinancetest.components.InvestmentViewMode
import com.example.appfinancetest.components.TopBar
import com.example.appfinancetest.components.saveTransactionAndSyncInvestments
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun InvestmentScreen(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    creditViewModel: CreditDBViewModel
) {
    val scope = rememberCoroutineScope()
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isVisibilityOff by prefs.isVisibilityOffFlow.collectAsState(initial = false)

    var isLoading by remember { mutableStateOf(true) }

    val isDarkThemeCustom by prefs.isDarkThemeFlow.collectAsState(initial = null)
    val darkTheme = isDarkThemeCustom ?: isSystemInDarkTheme()

    // Colors supporting Light & Dark mode
    val bgDark = if (darkTheme) BgDark else MaterialTheme.colorScheme.background
    val cardBg = if (darkTheme) CardBg else MaterialTheme.colorScheme.surface
    val cardBorder = if (darkTheme) CardBorder else MaterialTheme.colorScheme.outlineVariant
    val textPrimary = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface
    val textMuted = if (darkTheme) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant
    val bluePill = BluePill

    // State for tabs: 0 = "Current", 1 = "Closed"
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(id = R.string.investment_current),
        stringResource(id = R.string.investment_closed)
    )

    // State for view mode: HEATMAP vs LIST
    var viewMode by remember { mutableStateOf(InvestmentViewMode.HEATMAP) }

    // State for selected category (Object of Investment)
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    var isPrefsLoaded by remember { mutableStateOf(false) }

    // Load saved preferences ONCE at startup
    LaunchedEffect(Unit) {
        try {
            val savedTab = prefs.investmentSecondTabIndexFlow.first()
            val savedCategory = prefs.investmentSecondCategoryFlow.first()
            val savedViewMode = prefs.investmentSecondViewModeFlow.first()

            if (savedTab != null) {
                selectedTabIndex = savedTab
            }
            if (savedCategory != null) {
                selectedCategory = savedCategory
            }
            if (savedViewMode != null) {
                try {
                    viewMode = InvestmentViewMode.valueOf(savedViewMode)
                } catch (_: Exception) {
                    viewMode = InvestmentViewMode.HEATMAP
                }
            }
        } catch (_: Exception) {
            // Defaults
        } finally {
            isPrefsLoaded = true
        }
    }

    // Save preferences when state changes
    LaunchedEffect(selectedTabIndex, selectedCategory, viewMode, isPrefsLoaded) {
        if (isPrefsLoaded) {
            prefs.saveInvestmentSecondTabIndex(selectedTabIndex)
            if (selectedCategory != null) {
                prefs.saveInvestmentSecondCategory(selectedCategory!!)
            }
            prefs.saveInvestmentSecondViewMode(viewMode.name)
        }
    }

    // State for clicked investment position transactions dialog
    var selectedInvestmentTransactions by remember { mutableStateOf<List<TransactionDB>?>(null) }
    var selectedInvestmentLabel by remember { mutableStateOf<String?>(null) }
    var transactionToEdit by remember { mutableStateOf<TransactionDB?>(null) }
    var innerRefreshTrigger by remember { mutableIntStateOf(0) }

    val allInvestments by produceState(initialValue = emptyList(), investmentViewModel, refreshTrigger) {
        if (value.isEmpty()) {
            isLoading = true
        }
        value = investmentViewModel.getInvestment()
        isLoading = false
    }

    val allTransactions by produceState<List<TransactionDB>>(initialValue = emptyList(), databaseViewModel, refreshTrigger, innerRefreshTrigger) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    // Dynamic extraction of unique category items from both InvestmentDB and investment transactions
    val dynamicItems = remember(allInvestments, allTransactions) {
        val itemsFromInvestments = allInvestments.mapNotNull { it.item }.filter { it.isNotBlank() }
        val itemsFromTransactions = allTransactions
            .filter { it.category == "Investissement" || it.category == "Gain investissement" }
            .mapNotNull { it.item }
            .filter { it.isNotBlank() }
        (itemsFromInvestments + itemsFromTransactions).distinct().sorted()
    }

    // Ensure selectedCategory defaults to first available item if null or not present
    LaunchedEffect(dynamicItems, isPrefsLoaded) {
        if (isPrefsLoaded) {
            if (selectedCategory == null && dynamicItems.isNotEmpty()) {
                selectedCategory = dynamicItems.first()
            } else if (selectedCategory != null && selectedCategory !in dynamicItems && dynamicItems.isNotEmpty()) {
                selectedCategory = dynamicItems.first()
            }
        }
    }

    var targetInvestmentForRealtime by remember { mutableStateOf<Pair<String, List<TransactionDB>>?>(null) }

    val handleInvestmentClick: (InvestmentDB, List<TransactionDB>) -> Unit = { investment, transactions ->
        val label = investment.label ?: "Investment"
        val isBourseOrCrypto = transactions.any { tx ->
            val itemText = (tx.item ?: "").lowercase(Locale.ROOT)
            val labelText = (tx.label ?: "").lowercase(Locale.ROOT)
            !itemText.contains("crowd") && !labelText.contains("crowd")
        }
        if (isBourseOrCrypto) {
            targetInvestmentForRealtime = Pair(label, transactions)
        } else {
            selectedInvestmentLabel = label
            selectedInvestmentTransactions = transactions
        }
    }

    TransactionsLabelDialog(
        selectedLabel = selectedInvestmentLabel,
        othersTransactions = selectedInvestmentTransactions,
        isVisibilityOff = isVisibilityOff,
        databaseViewModel = databaseViewModel,
        onDismiss = {
            selectedInvestmentLabel = null
            selectedInvestmentTransactions = null
        },
        onTransactionClick = { transactionToEdit = it }
    )

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
                    if (!updated.item.isNullOrBlank()) {
                        selectedCategory = updated.item
                    }
                    innerRefreshTrigger++
                    refreshTrigger++
                    transactionToEdit = null
                }
            }
        )
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
    ) {
        if (targetInvestmentForRealtime != null) {
            StockMarketRealtimeDialog(
                databaseViewModel = databaseViewModel,
                investmentViewModel = investmentViewModel,
                isVisibilityOff = isVisibilityOff,
                targetTransactions = targetInvestmentForRealtime!!.second,
                targetLabel = targetInvestmentForRealtime!!.first,
                onImportExportClick = { showImportExport = true },
                onVisibilityClick = {
                    scope.launch {
                        prefs.saveVisibilityState(!isVisibilityOff)
                    }
                },
                onSettingsClick = { showSettings = true },
                modifier = Modifier.fillMaxSize(),
                onDismiss = { targetInvestmentForRealtime = null }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // HEADER
                TopBar(
                    title = stringResource(id = R.string.investments_title),
                    onImportExportClick = { showImportExport = true },
                    onVisibilityClick = {
                        scope.launch {
                            prefs.saveVisibilityState(!isVisibilityOff)
                        }
                    },
                    isVisibilityOff = isVisibilityOff,
                    onSettingsClick = { showSettings = true },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary
                )

                if (isLoading) {
                repeat(2) {
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
                        color = textMuted,
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
                // 1. Tab Switcher Current / Closed - pill selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(24.dp)),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTabIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) bluePill else Color.Transparent)
                                .clickable { selectedTabIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else textMuted,
                            )
                        }
                    }
                }

                // 2. Category Switcher (Objects of Investments)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(dynamicItems) { itemKey ->
                        val isSelected = selectedCategory == itemKey
                        Box(
                            modifier = Modifier
                                .width(130.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) bluePill else cardBg)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) bluePill else cardBorder
                                    ), RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedCategory = itemKey }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = itemKey,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) Color.White else textMuted,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Filter investments for selected category and tab
                val currentCategory = selectedCategory ?: dynamicItems.firstOrNull() ?: ""
                val categoryInvestments = allInvestments.filter { it.item == currentCategory }
                val filteredInvestments = remember(categoryInvestments, selectedTabIndex) {
                    if (selectedTabIndex == 0) {
                        categoryInvestments.filter { it.dateEnd == null || it.dateEnd == 0.0 }
                    } else {
                        categoryInvestments.filter { it.dateEnd != null && it.dateEnd != 0.0 }
                    }
                }

                val totalInvested = filteredInvestments.sumOf { it.invested ?: 0.0 }

                // 3. Invested amount card
                InvestmentSummaryCard(
                    totalInvested = totalInvested,
                    isVisibilityOff = isVisibilityOff,
                    selectedTabIndex = selectedTabIndex,
                    filteredInvestments = filteredInvestments,
                    allTransactions = allTransactions,
                    viewMode = viewMode,
                    onViewModeChange = { viewMode = it },
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textMuted = textMuted,
                    bluePill = bluePill
                )

                // Main container (fixe card)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    val isHeatmapActive =
                        selectedTabIndex == 0 && viewMode == InvestmentViewMode.HEATMAP
                    Text(
                        text = if (isHeatmapActive)
                            stringResource(id = R.string.heatmap_title)
                        else
                            stringResource(id = R.string.investment_list_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (filteredInvestments.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    stringResource(id = R.string.no_investment),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textMuted
                                )
                            }
                        } else if (isHeatmapActive) {
                            InvestmentHeatmapView(
                                filteredInvestments = filteredInvestments,
                                allTransactions = allTransactions,
                                isVisibilityOff = isVisibilityOff,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                onInvestmentClick = handleInvestmentClick
                            )
                        } else {
                            InvestmentListView(
                                filteredInvestments = filteredInvestments,
                                allTransactions = allTransactions,
                                isVisibilityOff = isVisibilityOff,
                                cardBg = cardBg,
                                cardBorder = cardBorder,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                databaseViewModel = databaseViewModel,
                                investmentViewModel = investmentViewModel,
                                selectedTabIndex = selectedTabIndex,
                                onRefresh = { refreshTrigger++ },
                                onInvestmentClick = handleInvestmentClick
                            )
                        }
                    }
                }
            }
        }
    }
}
}
