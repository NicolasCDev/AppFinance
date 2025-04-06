package com.example.appfinancetest

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.os.Bundle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.appfinancetest.ui.theme.AppFinanceTestTheme
import androidx.compose.runtime.collectAsState
import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.room.*
import androidx.activity.viewModels
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.appfinancetest.Transaction
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.filled.Settings
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppFinanceTestTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    var selectedItem by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val items = listOf("Tableau de bord", "Investissement", "Patrimoine", "DataBase")
    val icons = listOf(R.drawable.ic_dashboard, R.drawable.ic_investment, R.drawable.ic_patrimoine, R.drawable.ic_database)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Paramètres")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = icons[index]),
                                contentDescription = item
                            )
                        },
                        label = { Text(item) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index },
                        modifier = if(index == 0){
                             Modifier.weight(1f)
                        }else{
                            Modifier
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedItem) {
            0 -> DashboardScreen(modifier = Modifier.padding(innerPadding))
            1 -> InvestissementScreen(modifier = Modifier.padding(innerPadding))
            2 -> PatrimonialScreen(modifier = Modifier.padding(innerPadding))
            3 -> DataBaseScreen(modifier = Modifier.padding(innerPadding))
            else -> {
                ErrorScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
    if (showSettings) {
        SettingsScreen(onDismiss = { showSettings = false })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

fun readExcelFile(inputStream: InputStream): List<Transaction> {
    val transactions = mutableListOf<Transaction>()
    try {
        val workbook: Workbook = XSSFWorkbook(inputStream)
        val sheet = workbook.getSheetAt(0) // Récupérer la première feuille du fichier Excel

        for (row in sheet) {
            if (row.rowNum == 0) continue // Ignorer la première ligne (entêtes)

            val dateCell = row.getCell(0).dateCellValue
            val dateFormat = SimpleDateFormat("dd/MM/yy")
            val date = dateFormat.format(dateCell)
            val categorie = row.getCell(1).stringCellValue.toString()
            val poste = row.getCell(2).stringCellValue.toString()
            val label = row.getCell(3).stringCellValue.toString()
            val montant = row.getCell(4).numericCellValue.toDouble()

            // Ajouter une nouvelle transaction à la liste
            transactions.add(Transaction(date, categorie, poste, label, montant))
        }
    } catch (e: Exception) {
        e.printStackTrace() // Gérer les erreurs de lecture de fichier
    }

    // Ajouter un log pour vérifier les transactions lues
    Log.d("ExcelReader", "Transactions lues : ${transactions.size}")
    return transactions
}

fun writeExcelFile(outputStream: OutputStream, transactions: List<Transaction>) {
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("Transactions")

    // En-têtes
    val headerRow = sheet.createRow(0)
    headerRow.createCell(0).setCellValue("Date")
    headerRow.createCell(1).setCellValue("Catégorie")
    headerRow.createCell(2).setCellValue("Poste")
    headerRow.createCell(3).setCellValue("Libellé")
    headerRow.createCell(4).setCellValue("Montant")

    // Données des transactions
    for ((index, transaction) in transactions.withIndex()) {
        val row = sheet.createRow(index + 1)
        row.createCell(0).setCellValue(transaction.date)
        row.createCell(1).setCellValue(transaction.categorie)
        row.createCell(2).setCellValue(transaction.poste)
        row.createCell(3).setCellValue(transaction.label)
        row.createCell(4).setCellValue(transaction.montant)
    }

    // Écriture dans le OutputStream
    workbook.write(outputStream) // Cette ligne permet de sauvegarder le contenu du workbook dans le OutputStream

    // Libération des ressources
    workbook.close() // Fermer le workbook pour libérer les ressources
}

