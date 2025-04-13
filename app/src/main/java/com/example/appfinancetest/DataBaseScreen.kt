package com.example.appfinancetest

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DataBaseScreen(modifier: Modifier = Modifier, viewModel: DataBase_ViewModel) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    var showDialog by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var selectedFileUri by remember {
        mutableStateOf(sharedPreferences.getString("selected_file_uri", "") ?: "")
    }

    // Launcher pour choisir un fichier Excel
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                // Demander l'accès permanent au fichier sélectionné
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Sauvegarder l'URI dans SharedPreferences
                sharedPreferences.edit().putString("selected_file_uri", uri.toString()).apply()
                selectedFileUri = uri.toString()

                // Lire le fichier
                context.contentResolver.openInputStream(it)?.let { inputStream ->
                    addTransaction(readExcelFile(inputStream), viewModel)
                }
            } catch (e: SecurityException) {
                Log.e("FilePicker", "Erreur lors de l'accès au fichier : ${e.message}")
            }
        }
    }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choisir une option") },
            text = {
                Column() {
                    Button(onClick = {
                        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) // On va chercher le fichier
                        showDialog = false
                    }
                    ) {
                        Text("Ajouter")
                    }
                    Spacer(modifier = Modifier.padding(10.dp))
                    Button(onClick = {
                        if (selectedFileUri.isNotEmpty()) {
                            viewModel.deleteAllTransactions()
                            filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) // On va chercher le fichier
                            showDialog = false
                        }
                    }) {
                        Text("Remplacer")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) {
                    Text("Annuler")
                }
            }
        )
    }

    // Variables pour le filtrage
    var dateFilter by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("") }
    var posteFilter by remember { mutableStateOf("") }
    var labelFilter by remember { mutableStateOf("") }
    var amountFilter by remember { mutableStateOf("") }

    // Pagination
    val pageSize = 100 // Nombre de transactions à afficher par "page"
    val beforeRefresh = 20 // Nombre de transactions restant à afficher avant le refresh
    var currentPage by remember { mutableStateOf(1) } // Numéro de la page actuelle
    val transactionsToShow = remember { mutableStateListOf<Transaction_DB>() } // Liste des transactions à afficher

    // LazyListState pour suivre le défilement
    val listState = rememberLazyListState()
    // Flag pour vérifier si c'est le premier chargement
    var isFirstLoad by remember { mutableStateOf(true) }

    // Logique de pagination : charger les transactions lorsque l'on atteint la fin
    LaunchedEffect(listState) {
        delay(200) // Délai pour éviter de charger trop rapidement
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        }.collect { lastVisibleItemIndex ->
            if (lastVisibleItemIndex != null &&
                lastVisibleItemIndex >= transactionsToShow.size - beforeRefresh
            ) {
                Log.d("DataBaseScreen", "Next page")
                currentPage += 1
            }
        }
    }
    // Charger les transactions (initialement et lors de la pagination)

    LaunchedEffect(currentPage) {
        scope.launch {
            if (isFirstLoad) {
                Log.d("DataBaseScreen", "First Load")
                val newTransactions = viewModel.getPagedTransactions(pageSize, 0)

                // Si on veut filtrer après chargement depuis la DB :
                val filteredTransactions = filterTransactions(
                    newTransactions,
                    dateFilter,
                    categoryFilter,
                    posteFilter,
                    labelFilter,
                    amountFilter
                )
                transactionsToShow.clear() // Réinitialiser la liste avant d'ajouter
                transactionsToShow.addAll(filteredTransactions)
                // Une fois le premier chargement effectué, changer l'état
                isFirstLoad = false
            } else {
                Log.d("DataBaseScreen", "Not first Load")
                val offset = (currentPage - 1) * pageSize
                val newTransactions = viewModel.getPagedTransactions(pageSize, offset)
                // Appliquer les filtres
                val filteredTransactions = filterTransactions(
                    newTransactions,
                    dateFilter,
                    categoryFilter,
                    posteFilter,
                    labelFilter,
                    amountFilter
                )

                transactionsToShow.addAll(filteredTransactions)

            }
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Database",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "NBTransactions : ${transactionsToShow.size}",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                // Entête du tableau
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Category", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Poste", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Label", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Amount", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Variation", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                // Liste des transactions
                LazyColumn(state = listState) {
                    if (transactionsToShow.isEmpty()) {
                        item {
                            Text(
                                "Aucune transaction à afficher.",
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
                                Text(
                                    text = DateFormattedText(transactionDB.date),
                                    modifier = Modifier.weight(1f), fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    transactionDB.categorie ?: "N/A",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp
                                )
                                Text(
                                    transactionDB.poste ?: "N/A",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp
                                )
                                Text(
                                    transactionDB.label ?: "N/A",
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = String.format("%.2f €", transactionDB.montant ?: 0.0),
                                    modifier = Modifier.weight(1f), fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = String.format("%.2f €", transactionDB.variation ?: 0.0),
                                    modifier = Modifier.weight(1f), fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bouton pour choisir un fichier Excel
        Column(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
            // Bouton d'export
            AssistChip(
                onClick = {
                    exportFileLauncher.launch("export.xlsx")
                },
                label = { Text("Exporter") },
                leadingIcon = {
                    Icon(
                        painterResource(id = R.drawable.ic_export),
                        contentDescription = "Exporter les données",
                        modifier = Modifier.padding(end = 4.dp)
                    )
                },
                modifier = Modifier.padding(bottom = 10.dp),
                colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.primary)
            )
            // Floating action button pour ouvrir le fichier Excel
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        if (selectedFileUri.isNotEmpty()) {
                            showDialog = true
                        }
                    }
                },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(painterResource(id = R.drawable.ic_add_transac),  contentDescription = "Ajouter une transaction")
            }
        }
    }
}

