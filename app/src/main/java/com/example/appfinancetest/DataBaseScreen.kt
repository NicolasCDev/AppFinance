package com.example.appfinancetest

import android.Manifest
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun DataBaseScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val transactions = remember { mutableStateListOf<Transaction>() }
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
            // Demander l'accès permanent au fichier sélectionné
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Sauvegarder l'URI dans SharedPreferences
            sharedPreferences.edit().putString("selected_file_uri", uri.toString()).apply()
            selectedFileUri = uri.toString()

            // Lire le fichier
            context.contentResolver.openInputStream(it)?.let { inputStream ->
                transactions.addAll(readExcelFile(inputStream))
            }
        }
    }
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                writeExcelFile(outputStream, transactions)
            }
        }
    }

    // Lors du lancement de l'effet, on vérifie si un fichier a été sélectionné
    LaunchedEffect(selectedFileUri) {
        if (selectedFileUri.isNotEmpty()) {
            val uri = Uri.parse(selectedFileUri)

            // Demander l'accès permanent au fichier
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

            // Ouvrir et lire le fichier après avoir pris la permission persistante
            context.contentResolver.openInputStream(uri)?.let { inputStream ->
                transactions.addAll(readExcelFile(inputStream))
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
                        transactions.clear() // Remplacer toutes les transactions existantes
                        filePickerLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) // On va chercher le fichier
                        showDialog = false
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

    Box(modifier = modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(modifier = Modifier.fillMaxSize()) {
                Text(
                    text = "Database",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )

                // Entête du tableau
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Catégorie", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Poste", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Libellé", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text("Montant", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }

                // Liste des transactions
                LazyColumn {
                    items(transactions) { transaction ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(transaction.date, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(transaction.categorie, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(transaction.poste, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(transaction.label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp)
                            Text(
                                text = String.format("%.2f €", transaction.montant),
                                modifier = Modifier.weight(1f), fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
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
                        if(selectedFileUri.isNotEmpty()){
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
