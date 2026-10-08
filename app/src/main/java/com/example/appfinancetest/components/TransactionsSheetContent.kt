package com.example.appfinancetest.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.appfinancetest.R
import com.example.appfinancetest.calculations.filterTransactions
import com.example.appfinancetest.classes.DataBaseViewModel
import com.example.appfinancetest.classes.DataStorage
import com.example.appfinancetest.classes.InvestmentDBViewModel
import com.example.appfinancetest.classes.TransactionDB
import com.example.appfinancetest.ui.theme.BgDark
import com.example.appfinancetest.ui.theme.CardBg
import com.example.appfinancetest.ui.theme.CardBorder
import com.example.appfinancetest.ui.theme.TextMuted
import com.example.appfinancetest.views.TransactionEditDialog
import com.example.appfinancetest.views.TransactionFilterInterface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Scaffold complet avec volet coulissant pour les transactions.
 * Englobe BottomSheetScaffold, la gestion des couleurs, la poignée de glissement et le volet coulissant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsBottomSheetScaffold(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    isVisibilityOff: Boolean = false,
    refreshTrigger: Int = 0,
    onRefreshNeeded: () -> Unit = {},
    sheetPeekHeight: Dp = 180.dp,
    title: String = stringResource(id = R.string.recent_movements),
    maxHeightFraction: Float = 0.85f,
    transactionsList: List<TransactionDB>? = null,
    onClose: (() -> Unit)? = null,
    onTransactionClick: ((TransactionDB) -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isDarkThemeCustom by prefs.isDarkThemeFlow.collectAsState(initial = null)
    val darkTheme = isDarkThemeCustom ?: isSystemInDarkTheme()

    val bgDark = if (darkTheme) BgDark else MaterialTheme.colorScheme.background
    val cardBg = if (darkTheme) CardBg else MaterialTheme.colorScheme.surface
    val textPrimary = if (darkTheme) Color.White else MaterialTheme.colorScheme.onSurface

    val scaffoldState = rememberBottomSheetScaffoldState()

    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        sheetContainerColor = cardBg,
        sheetContentColor = textPrimary,
        sheetPeekHeight = sheetPeekHeight,
        sheetDragHandle = {
            BottomSheetDefaults.DragHandle()
        },
        sheetContent = {
            TransactionsSheetContent(
                databaseViewModel = databaseViewModel,
                investmentViewModel = investmentViewModel,
                isVisibilityOff = isVisibilityOff,
                refreshTrigger = refreshTrigger,
                onRefreshNeeded = onRefreshNeeded,
                title = title,
                maxHeightFraction = maxHeightFraction,
                transactionsList = transactionsList,
                onClose = onClose,
                onTransactionClick = onTransactionClick
            )
        },
        containerColor = bgDark,
        content = content
    )
}

/**
 * Composant réutilisable affichant le contenu du volet coulissant des transactions récentes
 * avec recherche, filtres, pagination et édition de transaction.
 */
@Composable
fun TransactionsSheetContent(
    modifier: Modifier = Modifier,
    databaseViewModel: DataBaseViewModel,
    investmentViewModel: InvestmentDBViewModel,
    isVisibilityOff: Boolean = false,
    refreshTrigger: Int = 0,
    onRefreshNeeded: () -> Unit = {},
    title: String = stringResource(id = R.string.recent_movements),
    maxHeightFraction: Float = 0.85f,
    transactionsList: List<TransactionDB>? = null,
    onClose: (() -> Unit)? = null,
    onTransactionClick: ((TransactionDB) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val prefs = remember { DataStorage(context) }
    val isDarkThemeCustom by prefs.isDarkThemeFlow.collectAsState(initial = null)
    val darkTheme = isDarkThemeCustom ?: isSystemInDarkTheme()

    val cardBorder = if (darkTheme) CardBorder else MaterialTheme.colorScheme.outlineVariant
    val textMuted = if (darkTheme) TextMuted else MaterialTheme.colorScheme.onSurfaceVariant

    // Filter states (all initialized to empty)
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

    var transactionToEdit by remember { mutableStateOf<TransactionDB?>(null) }
    var internalRefreshTrigger by remember { mutableIntStateOf(0) }

    val combinedRefreshTrigger = refreshTrigger + internalRefreshTrigger

    LaunchedEffect(searchQuery) {
        labelFilter = searchQuery
    }

    val categoriesList by produceState(initialValue = emptyList(), databaseViewModel, combinedRefreshTrigger) {
        value = databaseViewModel.getAllCategories()
    }
    val itemsList by produceState(initialValue = emptyList(), databaseViewModel, combinedRefreshTrigger) {
        value = databaseViewModel.getAllItems()
    }
    val labelsList by produceState(initialValue = emptyList(), databaseViewModel, combinedRefreshTrigger) {
        value = databaseViewModel.getAllLabels()
    }

    // Pagination for transactions list
    val pageSize = 50
    val beforeRefresh = 15
    var currentPage by remember { mutableIntStateOf(1) }
    val transactionsPaged = remember { mutableStateListOf<TransactionDB>() }
    val listState = rememberLazyListState()
    var isFirstLoadPaged by remember { mutableStateOf(true) }

    val effectiveLabelFilter = searchQuery.ifBlank { labelFilter }
    val isFilterActive = dateMinFilter.isNotBlank() ||
            dateMaxFilter.isNotBlank() ||
            categoryFilter.isNotBlank() ||
            itemFilter.isNotBlank() ||
            effectiveLabelFilter.isNotBlank() ||
            amountMinFilter.isNotBlank() ||
            amountMaxFilter.isNotBlank()

    LaunchedEffect(
        listState, dateMinFilter, dateMaxFilter, categoryFilter, itemFilter, labelFilter,
        amountMinFilter, amountMaxFilter, searchQuery, transactionsList, combinedRefreshTrigger
    ) {
        val currentLabelFilter = if (searchQuery.isNotBlank()) searchQuery else labelFilter
        val hasActiveFilter = dateMinFilter.isNotBlank() ||
                dateMaxFilter.isNotBlank() ||
                categoryFilter.isNotBlank() ||
                itemFilter.isNotBlank() ||
                currentLabelFilter.isNotBlank() ||
                amountMinFilter.isNotBlank() ||
                amountMaxFilter.isNotBlank()

        if (hasActiveFilter) {
            val baseList = transactionsList ?: databaseViewModel.getTransactionsSortedByDateDESC()
            val filtered = filterTransactions(
                baseList,
                dateMinFilter,
                dateMaxFilter,
                categoryFilter,
                itemFilter,
                currentLabelFilter,
                amountMinFilter,
                amountMaxFilter
            )
            transactionsPaged.clear()
            transactionsPaged.addAll(filtered)
            isFirstLoadPaged = false
        } else if (transactionsList != null) {
            transactionsPaged.clear()
            transactionsPaged.addAll(transactionsList.sortedByDescending { it.date })
            isFirstLoadPaged = false
        } else {
            delay(200.milliseconds)
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

    LaunchedEffect(
        currentPage, combinedRefreshTrigger, dateMinFilter, dateMaxFilter, categoryFilter, itemFilter,
        labelFilter, amountMinFilter, amountMaxFilter, searchQuery, transactionsList
    ) {
        val currentLabelFilter = if (searchQuery.isNotBlank()) searchQuery else labelFilter
        val hasActiveFilter = dateMinFilter.isNotBlank() ||
                dateMaxFilter.isNotBlank() ||
                categoryFilter.isNotBlank() ||
                itemFilter.isNotBlank() ||
                currentLabelFilter.isNotBlank() ||
                amountMinFilter.isNotBlank() ||
                amountMaxFilter.isNotBlank()

        if (!hasActiveFilter) {
            if (transactionsList != null) {
                transactionsPaged.clear()
                transactionsPaged.addAll(transactionsList.sortedByDescending { it.date })
                isFirstLoadPaged = false
            } else {
                val offset = (currentPage - 1) * pageSize
                val newTransactions = databaseViewModel.getPagedTransactions(pageSize, offset)
                if (currentPage == 1) {
                    transactionsPaged.clear()
                }
                transactionsPaged.addAll(newTransactions)
                isFirstLoadPaged = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .fillMaxHeight(maxHeightFraction)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
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

                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textMuted
                        )
                    }
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
                            if (onTransactionClick != null) {
                                onTransactionClick(item)
                            } else {
                                transactionToEdit = item
                            }
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
                    internalRefreshTrigger++
                    onRefreshNeeded()
                    transactionToEdit = null
                }
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
