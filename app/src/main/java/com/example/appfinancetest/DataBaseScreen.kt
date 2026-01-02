package com.example.appfinancetest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBaseScreen(modifier: Modifier = Modifier, databaseViewModel: DataBaseViewModel, investmentViewModel: InvestmentDB_ViewModel) {
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

    var showValidation by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showImportExport by remember { mutableStateOf(false) }

    if (showValidation) {
        InvestmentValidationInterface(databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel, onDismiss = { showValidation = false })
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
//    if (showImportExport) {
//        ImportExportInterface(databaseViewModel = databaseViewModel, investmentViewModel = investmentViewModel, onDismiss = { showImportExport = false }, onRefresh = {
//            refreshTrigger++
//            isFirstLoad = true
//            currentPage = 1
//        })
//    }

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
            }
        }
    )
}
