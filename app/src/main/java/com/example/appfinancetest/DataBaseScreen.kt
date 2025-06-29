package com.example.appfinancetest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBaseScreen(modifier: Modifier = Modifier, databaseViewModel: DataBase_ViewModel, investmentViewModel: InvestmentDB_ViewModel) {
    val scope = rememberCoroutineScope()

    var refreshTrigger by remember { mutableIntStateOf(0) }
    var dateFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var amountFilter by remember { mutableStateOf("") }

    val pageSize = 100
    val beforeRefresh = 40
    var currentPage by remember { mutableIntStateOf(1) }
    val transactionsToShow = remember { mutableStateListOf<TransactionDB>() }
    val listState = rememberLazyListState()
    var isFirstLoad by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        delay(200)
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleItemIndex ->
            if (lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= transactionsToShow.size - beforeRefresh
            ) {
                currentPage += 1
            }
        }
    }

    LaunchedEffect(currentPage, refreshTrigger) {
        scope.launch {
            val offset = (currentPage - 1) * pageSize
            val newTransactions = databaseViewModel.getPagedTransactions(pageSize, offset)
            val filtered = filterTransactions(
                newTransactions,
                dateFilter, categoryFilter, itemFilter, labelFilter, amountFilter
            )
            if (isFirstLoad) {
                transactionsToShow.clear()
                isFirstLoad = false
            }
            transactionsToShow.addAll(filtered)
        }
    }

    var showValidation by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    if (showValidation) {
        InvestmentValidationInterface(databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel, onDismiss = { showValidation = false })
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
    if (showImportExport) {
        ImportExportInterface(databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel, onDismiss = { showImportExport = false }, onRefresh = {
            refreshTrigger++
            isFirstLoad = true
            currentPage = 1
        })
    }

    Scaffold(
        topBar = {
            TopBar(
                onValidateClick = { showValidation = true },
                onSettingsClick = { showSettings = true },
                onImportExportClick = { showImportExport = true },
                name = "DataBase"
            )
        },
        content = { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text(
                    text = "NBTransactions : ${transactionsToShow.size}",
                    modifier = Modifier
                        .padding(bottom = 8.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf(
                        "Date",
                        "Category",
                        "Item",
                        "Label",
                        "Amount"
                    ).forEach {
                        Text(
                            it,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                LazyColumn(state = listState) {
                    if (transactionsToShow.isEmpty()) {
                        item {
                            Text(
                                "No transaction to print",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        items(transactionsToShow) { transactionDB ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                val data = listOf(
                                    dateFormattedText(transactionDB.date),
                                    transactionDB.category ?: "N/A",
                                    transactionDB.item?: "N/A",
                                    transactionDB.label ?: "N/A",
                                    "%.2f €".format(transactionDB.amount ?: 0.0)
                                )
                                data.forEach {
                                    Text(
                                        it,
                                        modifier = Modifier.weight(1f),
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
