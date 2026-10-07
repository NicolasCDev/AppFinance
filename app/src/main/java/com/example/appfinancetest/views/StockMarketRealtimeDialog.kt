package com.example.appfinancetest.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.appfinancetest.R
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.components.StockMarketEvolutionChart
import com.example.appfinancetest.components.TransactionsBottomSheetScaffold
import com.example.appfinancetest.components.TopBar
import com.example.appfinancetest.services.StockMarketService
import com.example.appfinancetest.services.StockTickerData
import com.example.appfinancetest.ui.theme.BgDark
import com.example.appfinancetest.ui.theme.BluePill
import com.example.appfinancetest.ui.theme.CardBg
import com.example.appfinancetest.ui.theme.CardBorder
import com.example.appfinancetest.ui.theme.GreenAccent
import java.util.Locale

data class DetectedStockPosition(
    val isin: String?,
    val ticker: String,
    val displayLabel: String,
    val category: String,
    val transactions: List<TransactionDB>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockMarketRealtimeDialog(
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel? = null,
    isVisibilityOff: Boolean = false,
    initialCategory: String = "",
    targetTransactions: List<TransactionDB>? = null,
    targetLabel: String? = null,
    onImportExportClick: () -> Unit = {},
    onVisibilityClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val investVM: InvestmentDBViewModel = investmentViewModel ?: viewModel()
    val screenScrollState = rememberScrollState()

    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isDarkThemeCustom by prefs.isDarkThemeFlow.collectAsState(initial = null)
    val darkTheme = isDarkThemeCustom ?: isSystemInDarkTheme()

    val bgDark = if (darkTheme) BgDark else MaterialTheme.colorScheme.background
    val cardBg = if (darkTheme) CardBg else MaterialTheme.colorScheme.surface
    val cardBorder = if (darkTheme) CardBorder else MaterialTheme.colorScheme.outlineVariant
    val textPrimary = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface

    var selectedRange by remember { mutableStateOf("max") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val tickerDataMap = remember { mutableStateMapOf<String, StockTickerData>() }
    val transactionTickerMap = remember { mutableStateMapOf<TransactionDB, String>() }

    // Fetch all transactions from database
    val allTransactions by produceState<List<TransactionDB>>(initialValue = emptyList(), databaseViewModel) {
        value = databaseViewModel.getTransactionsSortedByDateASC()
    }

    // Filter stock market & crypto investment transactions
    val stockTransactions = remember(allTransactions, initialCategory, targetTransactions, targetLabel) {
        val baseList = targetTransactions ?: allTransactions
        baseList.filter { tx ->
            if (tx.amount == null || tx.amount <= 0 || tx.date == null) return@filter false
            val itemText = (tx.item ?: "").lowercase(Locale.ROOT)
            val labelText = (tx.label ?: "").lowercase(Locale.ROOT)
            if (itemText.contains("crowd") || labelText.contains("crowd")) return@filter false

            if (!targetLabel.isNullOrBlank()) {
                val matchesLabel = tx.label?.equals(targetLabel, ignoreCase = true) == true ||
                        tx.label?.contains(targetLabel, ignoreCase = true) == true ||
                        tx.item?.contains(targetLabel, ignoreCase = true) == true
                if (!matchesLabel) return@filter false
            }

            if (initialCategory.isNotBlank() && !initialCategory.equals("Bourse", ignoreCase = true) && !initialCategory.equals("Crypto", ignoreCase = true)) {
                val matchesCategory = tx.item?.equals(initialCategory, ignoreCase = true) == true ||
                        tx.item?.contains(initialCategory, ignoreCase = true) == true ||
                        tx.category?.equals(initialCategory, ignoreCase = true) == true ||
                        tx.category?.contains(initialCategory, ignoreCase = true) == true
                if (!matchesCategory) return@filter false
            }

            true
        }
    }

    // Group stock/crypto transactions by detected ISIN or label
    val detectedPositions = remember(stockTransactions) {
        val groups = stockTransactions.groupBy { tx ->
            val labelText = "${tx.label ?: ""} ${tx.item ?: ""}"
            val extractedIsin = StockMarketService.extractIsin(labelText)
            extractedIsin ?: tx.label ?: tx.item ?: "Position"
        }

        groups.map { (key, txs) ->
            val isin = StockMarketService.extractIsin(key)
            val displayLabel = txs.firstOrNull()?.label ?: key
            val cat = txs.firstOrNull()?.item ?: "Bourse"
            DetectedStockPosition(
                isin = isin,
                ticker = isin ?: "CW8.PA",
                displayLabel = displayLabel,
                category = cat,
                transactions = txs
            )
        }.sortedByDescending { pos -> pos.transactions.sumOf { it.amount ?: 0.0 } }
    }

    // Load tickers for detected positions
    LaunchedEffect(detectedPositions, selectedRange, refreshTrigger) {
        isLoading = true
        errorMessage = null

        try {
            for (pos in detectedPositions) {
                val tickerToUse = if (pos.isin != null) {
                    StockMarketService.resolveIsinToTicker(pos.isin)
                } else {
                    val labelLower = pos.displayLabel.lowercase(Locale.ROOT)
                    StockMarketService.inferCryptoTicker(labelLower)
                        ?: if (labelLower.contains("world") || labelLower.contains("cw8")) "CW8.PA"
                        else if (labelLower.contains("s&p") || labelLower.contains("500")) "ESE.PA"
                        else if (labelLower.contains("cac")) "^FCHI"
                        else "CW8.PA"
                }

                pos.transactions.forEach { tx ->
                    transactionTickerMap[tx] = tickerToUse
                }

                if (!tickerDataMap.containsKey(tickerToUse) || refreshTrigger > 0) {
                    val rangeParam = when (selectedRange) {
                        "1d" -> "5d"
                        "1w" -> "1mo"
                        "1m" -> "1mo"
                        "6m" -> "6mo"
                        "1y" -> "1y"
                        else -> "max"
                    }
                    val res = StockMarketService.fetchTickerData(tickerToUse, range = rangeParam)
                    if (res.isSuccess) {
                        res.getOrNull()?.let { tickerDataMap[tickerToUse] = it }
                    }
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Erreur de chargement"
        } finally {
            isLoading = false
        }
    }

    val activeTransactions = stockTransactions
    val totalInvestedAmount = activeTransactions.sumOf { it.amount ?: 0.0 }

    // First detected ticker data for header price display
    val primaryTicker = transactionTickerMap[activeTransactions.firstOrNull()] ?: "CW8.PA"

    // Compute shares and estimated value
    var estimatedCurrentPortfolioValue = 0.0
    var totalSharesCount = 0.0

    for (tx in activeTransactions) {
        val ticker = transactionTickerMap[tx] ?: primaryTicker
        val tData = tickerDataMap[ticker]
        val priceHist = tData?.history?.sortedBy { it.timestampMilli } ?: emptyList()
        val txMillis = ((tx.date ?: 0.0) - 25569.0) * 86400.0 * 1000.0

        val buyPrice = priceHist.firstOrNull { it.timestampMilli >= txMillis }?.closePrice
            ?: priceHist.lastOrNull()?.closePrice
            ?: tData?.currentPrice
            ?: 1.0

        val shares = (tx.amount ?: 0.0) / buyPrice
        totalSharesCount += shares
        estimatedCurrentPortfolioValue += shares * (tData?.currentPrice ?: buyPrice)
    }

    val latentGainEuro = estimatedCurrentPortfolioValue - totalInvestedAmount
    val latentGainPct = if (totalInvestedAmount > 0) (latentGainEuro / totalInvestedAmount) * 100.0 else 0.0
    val weightedAverageBuyPrice = if (totalSharesCount > 0) totalInvestedAmount / totalSharesCount else 0.0

    val ranges = listOf(
        "1d" to "1J",
        "1w" to "1S",
        "1m" to "1M",
        "6m" to "6M",
        "1y" to "1A",
        "max" to "ALL"
    )

    // Calculate start time millis filter based on selectedRange
    val nowMillis = System.currentTimeMillis()
    val startTimeMillis = remember(selectedRange, nowMillis) {
        when (selectedRange) {
            "1d" -> nowMillis - 86400000L * 1
            "1w" -> nowMillis - 86400000L * 7
            "1m" -> nowMillis - 86400000L * 30
            "6m" -> nowMillis - 86400000L * 180
            "1y" -> nowMillis - 86400000L * 365
            else -> 0L
        }
    }

    TransactionsBottomSheetScaffold(
        modifier = modifier.fillMaxSize(),
        databaseViewModel = databaseViewModel,
        investmentViewModel = investVM,
        isVisibilityOff = isVisibilityOff,
        refreshTrigger = refreshTrigger,
        onRefreshNeeded = {
            refreshTrigger++
        },
        title = stringResource(id = R.string.recent_movements),
        transactionsList = activeTransactions,
        sheetPeekHeight = 180.dp
    ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(screenScrollState)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TOP BAR (Main Screen TopBar)
                TopBar(
                    title = stringResource(id = R.string.investments_title),
                    onImportExportClick = onImportExportClick,
                    onVisibilityClick = onVisibilityClick,
                    isVisibilityOff = isVisibilityOff,
                    onSettingsClick = onSettingsClick,
                    cardBg = cardBg,
                    cardBorder = cardBorder,
                    textPrimary = textPrimary
                )

                // 1. SUB-HEADER ("Détail de l'actif" + Back arrow + Refresh)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = textPrimary
                        )
                    }
                    Text(
                        text = "Détail de l'actif",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rafraîchir",
                            tint = textPrimary
                        )
                    }
                }

                // 2. ASSET HEADER INFO
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BluePill),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (targetLabel ?: "A").take(1).uppercase(Locale.ROOT),
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = targetLabel ?: "Actif Boursier",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = primaryTicker,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val detectedIsin = detectedPositions.firstOrNull()?.isin ?: "ISIN"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(cardBg)
                                        .border(1.dp, cardBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = detectedIsin,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isVisibilityOff) "**** €" else String.format(Locale.getDefault(), "%,.2f €", estimatedCurrentPortfolioValue),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val isPositive = latentGainEuro >= 0
                            val sign = if (isPositive) "+" else ""
                            val changeColor = if (isPositive) GreenAccent else MaterialTheme.colorScheme.error
                            Text(
                                text = if (isVisibilityOff) "+0.0%" else String.format(Locale.getDefault(), "%s%.1f%% (%s%,.2f €)", sign, latentGainPct, sign, latentGainEuro),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = changeColor
                            )
                        }
                    }
                }

                // 3. TIME RANGE PILLS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(cardBg)
                        .border(1.dp, cardBorder, RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ranges.forEach { (code, label) ->
                        val isSelected = selectedRange == code
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) BluePill else Color.Transparent)
                                .clickable { selectedRange = code }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 4. CHART AREA
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardBg)
                        .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BluePill)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Chargement du graphique...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else if (errorMessage != null) {
                        Text(errorMessage ?: "Erreur", color = MaterialTheme.colorScheme.error)
                    } else if (activeTransactions.isEmpty()) {
                        Text("Aucune transaction.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        StockMarketEvolutionChart(
                            stockTransactions = activeTransactions,
                            tickerDataMap = tickerDataMap,
                            transactionTickerMap = transactionTickerMap,
                            startTimeMillis = startTimeMillis,
                            isVisibilityOff = isVisibilityOff,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 5. "Informations"
                Text(
                    text = "Informations",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InfoKeyCard(
                        title = "PRU",
                        value = if (isVisibilityOff) "**** €" else String.format(Locale.getDefault(), "%,.2f €", weightedAverageBuyPrice),
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        modifier = Modifier.weight(1f)
                    )
                    InfoKeyCard(
                        title = "Nb d'unité",
                        value = if (isVisibilityOff) "****" else String.format(Locale.getDefault(), "%.3f", totalSharesCount),
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        modifier = Modifier.weight(1f)
                    )
                    InfoKeyCard(
                        title = "Plus-value",
                        value = if (isVisibilityOff) "**** €" else String.format(Locale.getDefault(), "%,.2f €", latentGainEuro),
                        cardBg = cardBg,
                        cardBorder = cardBorder,
                        isPositive = latentGainEuro >= 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(180.dp))
            }
        }
}

@Composable
fun InfoKeyCard(
    title: String,
    value: String,
    cardBg: Color,
    cardBorder: Color,
    isPositive: Boolean? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val color = when (isPositive) {
                true -> GreenAccent
                false -> MaterialTheme.colorScheme.error
                null -> MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
