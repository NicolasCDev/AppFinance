package com.example.appfinancetest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataBaseScreen(modifier: Modifier = Modifier, databaseViewModel: DataBase_ViewModel, investmentViewModel: InvestmentDB_ViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val scope = rememberCoroutineScope()

    var showDialog by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var dateFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var itemFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var amountFilter by remember { mutableStateOf("") }

    val pageSize = 100
    val beforeRefresh = 20
    var currentPage by remember { mutableIntStateOf(1) }
    val transactionsToShow = remember { mutableStateListOf<TransactionDB>() }
    val listState = rememberLazyListState()
    var isFirstLoad by remember { mutableStateOf(true) }
    var selectedFileUri by remember {
        mutableStateOf(sharedPreferences.getString("selected_file_uri", "") ?: "")
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                sharedPreferences.edit().putString("selected_file_uri", uri.toString()).apply()
                selectedFileUri = uri.toString()

                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    val file = readExcelFile(inputStream)
                    (context as? ComponentActivity)?.lifecycleScope?.launch {
                        addTransaction(file, databaseViewModel)
                        addInvestments(databaseViewModel, investmentViewModel)
                        refreshTrigger++
                        isFirstLoad = true
                        currentPage = 1
                    }
                }
            } catch (e: SecurityException) {
                Log.e("FilePicker", "Error : ${e.message}")
            }
        }
    }

    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { /* TODO: Écriture */ }
        }
    }

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

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose an option") },
            text = {
                Column {
                    Button(onClick = {
                        (context as? ComponentActivity)?.lifecycleScope?.launch {
                            investmentViewModel.deleteAllInvestments()
                            filePickerLauncher.launch(
                                arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            )
                            showDialog = false
                        }
                    }) { Text("Add") }

                    Spacer(modifier = Modifier.padding(10.dp))

                    Button(onClick = {
                        (context as? ComponentActivity)?.lifecycleScope?.launch {
                            databaseViewModel.deleteAllTransactions()
                            investmentViewModel.deleteAllInvestments()
                            // We launch the filePicker
                            filePickerLauncher.launch(
                                arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                            )
                            showDialog = false
                        }
                    }) { Text("Replace") }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "DataBase",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 18.dp)
                    )
                },
                navigationIcon = {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AssistChip(
                            onClick = { exportFileLauncher.launch("export.xlsx") },
                            label = { Text("Export") },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_export),
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FloatingActionButton(
                            onClick = {
                                scope.launch {
                                    showDialog = true
                                }
                            },
                            modifier = Modifier.size(40.dp),
                            containerColor = Color.White,
                            contentColor = Color.Blue,
                            elevation = FloatingActionButtonDefaults.elevation(4.dp)
                        ) {
                            Icon(
                                painterResource(id = R.drawable.ic_add_transac),
                                contentDescription = "Add",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding().coerceAtMost(8.dp))
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
                        "Amount",
                        "Variation",
                        "Balance"
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
                                    DateFormattedText(transactionDB.date),
                                    transactionDB.category ?: "N/A",
                                    transactionDB.item?: "N/A",
                                    transactionDB.label ?: "N/A",
                                    "%.2f €".format(transactionDB.amount ?: 0.0),
                                    "%.2f €".format(transactionDB.variation ?: 0.0),
                                    "%.2f €".format(transactionDB.balance ?: 0.0)
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
